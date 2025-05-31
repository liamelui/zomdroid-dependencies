package com.zomdroid.agent.glsl;

import com.zomdroid.agent.glsl.antlr.GLSLLexer;
import com.zomdroid.agent.glsl.antlr.GLSLParser;
import com.zomdroid.agent.glsl.antlr.GLSLParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A visitor that analyzes a GLSL shader and collects replacements to adapt it for GLSL ES compatibility.
 * This visitor performs two main tasks:
 * <ul>
 *   <li>Identifies integer literals used in floating-point contexts and prepares replacements with float literals (e.g., <code>1</code> → <code>1.0</code>).</li>
 *   <li>Detects user-defined functions that override built-in GLSL functions and marks them for removal.</li>
 * </ul>
 * The visitor uses a top-down propagation approach to determine context from parent nodes.
 * It does not modify the source directly; instead, it builds a list of token-level replacements
 * that can be applied in a separate pass to generate the final modified shader source.
 */
// TODO handle return statements (visitJump_statement). E.g if we have return 3 * var; and var is a float, 3 -> 3.0
// TODO constant fold uniform initializers to allow processing in gl4es. E.g. uniform int uni = 1 + 2; -> uniform int uni = 3;
// TODO handle struct declaration
public class GLSLCoreToESVisitor extends GLSLParserBaseVisitor<Void> {
    private final SymbolTable symbols = new SymbolTable();
    ParseTreeProperty<Boolean> floatContext = new ParseTreeProperty<>();

    // map tokenIndex -> replacement text
    public final Map<Integer, String> replacements = new HashMap<>();

    // map depth -> function param float context stack
    private final Map<Integer, LinkedList<Boolean>> depthToFuncParamFloatContextStack = new HashMap<>();

    private int depth = 0;

    private final GLSLBottomUpContextResolver bottomUpContextResolver;

    public GLSLCoreToESVisitor() {
        this.bottomUpContextResolver = new GLSLBottomUpContextResolver(symbols);
    }

    private void debugLog(String message) {
        String indent = "\t".repeat(depth);
        //System.out.println(indent + message);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        Token tok = node.getSymbol();
        debugLog("visitTerminal " + tok.getText());

        Boolean isInFloatContext = floatContext.get(node.getParent());
        if (tok.getType() == GLSLLexer.INTCONSTANT && isInFloatContext) {

            replacements.put(tok.getTokenIndex(), tok.getText() + ".0");

            int line = tok.getLine();
            int col = tok.getCharPositionInLine();
            debugLog(String.format("Replaced int constant at line %d, column %d: '%s' -> '%s.0'%n",
                    line, col, tok.getText(), tok.getText()));
        }
        return super.visitTerminal(node);
    }

    @Override
    public Void visitSingle_declaration(GLSLParser.Single_declarationContext ctx) {
        debugLog("visitSingle_declaration " + ctx.getText());

        GLSLType varType = null;
        GLSLParser.Fully_specified_typeContext fullySpecTypeCtx = ctx.fully_specified_type();
        if (fullySpecTypeCtx != null) {
            GLSLParser.Type_specifierContext typeCtx = fullySpecTypeCtx.type_specifier();
            if (typeCtx != null) {
                String varTypeName = typeCtx.getText();
                varType = symbols.getTypeByName(varTypeName);
            }
        }

        String varName = null;
        GLSLParser.Typeless_declarationContext typelessDeclCtx = ctx.typeless_declaration();
        if (typelessDeclCtx != null) {
            TerminalNode identifier = typelessDeclCtx.IDENTIFIER();
            if (identifier != null) {
                varName = identifier.getText();
            }
        }

        if (varType != null && varName != null) {
            symbols.declareVar(varName, varType);
            debugLog("Declared var with name=" + varName + " and type=" + varType.getName());
            floatContext.put(ctx, varType.getName().equals("float"));
        }

        depth++;
        super.visitSingle_declaration(ctx);
        depth--;

        return null;
    }

    @Override
    public Void visitAssignment_expression(GLSLParser.Assignment_expressionContext ctx) {
        debugLog("visitAssignment_expression " + ctx.getText());

        boolean isFloatContext = false;
        boolean isFunctionParam = false;

        LinkedList<Boolean> funcParamFloatContextStack = depthToFuncParamFloatContextStack.get(depth);
        if (funcParamFloatContextStack != null) {
            isFunctionParam = true;
            if (!funcParamFloatContextStack.isEmpty()) {
                isFloatContext = funcParamFloatContextStack.pollFirst();
            }
        }


        GLSLType varType = getVarTypeFromAssignment(ctx);
        if (varType != null) {
            /* Resolving float context based on vector type is not exactly necessary, since vectors allow int ops to some extent,
             * but this helps in situations like
             * vec2 a = vec2(1.0, 1.0);
             * a *= some_expr + 1;
             * Where without such resolution, we would need to visit entire some_expr tree to figure out the context
             * */
            isFloatContext = varType.getName().equals("float")
                    || varType.getName().equals("vec2") || varType.getName().equals("vec3") || varType.getName().equals("vec4");
        }

        if (!isFloatContext) {
            isFloatContext = bottomUpContextResolver.visit(ctx);
        }

        boolean wasFloatContext = isParentInFloatContext(ctx);

        // we are not interested in previous context if this is func param assignment
        floatContext.put(ctx, (wasFloatContext && !isFunctionParam) || isFloatContext);
        depth++;
        super.visitAssignment_expression(ctx);
        depth--;

        return null;
    }

    // some global vars are not defined explicitly (e.g. gl_FragColor) so they are not in symbols table TODO add predefined global vars to symbols table
    private GLSLType getVarTypeFromAssignment(GLSLParser.Assignment_expressionContext ctx) {
        GLSLParser.Unary_expressionContext unaryExpr = ctx.unary_expression();
        if (unaryExpr == null) return null;

        GLSLParser.Postfix_expressionContext postfixExpr = unaryExpr.postfix_expression();
        if (postfixExpr == null) return null;

        if (postfixExpr.field_selection() == null) return symbols.getVarType(postfixExpr.getText());


        ArrayList<String> fields = new ArrayList<>();
        while (postfixExpr.field_selection() != null) {
            fields.add(0, postfixExpr.field_selection().getText());
            postfixExpr = postfixExpr.postfix_expression();
            if (postfixExpr == null) return null;
        }

        GLSLType type = symbols.getVarType(postfixExpr.getText());

        for (String field : fields) {
            if (type instanceof GLSLType.StructType) {
                type = ((GLSLType.StructType) type).getFieldType(field);
            }
        }
        return type;
    }

    @Override
    public Void visitPostfix_expression(GLSLParser.Postfix_expressionContext ctx) {
        debugLog("visitPostfix_expression " + ctx.getText());

        List<ParseTree> children = new ArrayList<>();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            children.add(ctx.getChild(i));
        }
        int numChildren = children.size();

        boolean isFunctionCall = numChildren >= 3
                && !(children.get(0) instanceof GLSLParser.Type_specifierContext)
                && "(".equals(children.get(1).getText())
                && ")".equals(children.get(numChildren - 1).getText());

        LinkedList<Boolean> functionParamsFloatContextStack = new LinkedList<>();

        if (isFunctionCall) {
            String funcName = children.get(0).getText();

            GLSLParser.Function_call_parametersContext paramsCtx = null;
            for (ParseTree child : children) {
                if (child instanceof GLSLParser.Function_call_parametersContext) {
                    paramsCtx = (GLSLParser.Function_call_parametersContext) child;
                    break;
                }
            }

            List<String> actualParamTypeNames = new ArrayList<>();
            if (paramsCtx != null) {
                List<GLSLParser.Assignment_expressionContext> paramExprs = paramsCtx.assignment_expression();
                for (GLSLParser.Assignment_expressionContext expr : paramExprs) {
                    actualParamTypeNames.add(inferTypeName(expr));
                }
            }

            functionParamsFloatContextStack = resolveFunctionParamsContext(funcName, actualParamTypeNames);
        }

        depth++;

        if (isFunctionCall) {
            depthToFuncParamFloatContextStack.put(depth + 1, functionParamsFloatContextStack);
        }

        floatContext.put(ctx, isParentInFloatContext(ctx));

        super.visitPostfix_expression(ctx);

        if (isFunctionCall) {
            depthToFuncParamFloatContextStack.remove(depth + 1);
        }

        depth--;

        return null;
    }

    @Override
    public Void visitInteger_expression(GLSLParser.Integer_expressionContext ctx) {
        debugLog("visitInteger_expression " + ctx.getText());
        depth++;
        super.visitInteger_expression(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitFunction_prototype(GLSLParser.Function_prototypeContext ctx) {
        debugLog("visitFunction_prototype " + ctx.getText());
        depth++;
        super.visitFunction_prototype(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitFunction_definition(GLSLParser.Function_definitionContext ctx) {
        debugLog("visitFunction_definition " + ctx.getText());
        if (isBuiltinOverride(ctx.function_prototype())) {
            // remove function definition
            Token startToken = ctx.getStart();
            Token stopToken = ctx.getStop();
            for (int i = startToken.getTokenIndex(); i <= stopToken.getTokenIndex(); i++) {
                replacements.put(i, "");
            }
            return null;
        }

        symbols.enterNewVarScope();

        GLSLParser.Function_prototypeContext prototypeCtx = ctx.function_prototype();

        GLSLParser.Fully_specified_typeContext fullSpecTypeCtx = prototypeCtx.fully_specified_type();
        GLSLParser.Type_specifierContext typeSpecCtx = fullSpecTypeCtx.type_specifier();
        String typeName = typeSpecCtx.getText();
        GLSLType type = symbols.getTypeByName(typeName);

        String name = prototypeCtx.IDENTIFIER().getText();

        ArrayList<GLSLType> paramTypes = new ArrayList<>();

        GLSLParser.Function_parametersContext parametersCtx = prototypeCtx.function_parameters();
        if (parametersCtx != null) {
            List<GLSLParser.Parameter_declarationContext> parameterDeclarationCtxList = parametersCtx.parameter_declaration();
            for (GLSLParser.Parameter_declarationContext paramDeclarationCtx : parameterDeclarationCtxList) {
                GLSLParser.Parameter_declaratorContext paramDeclaratorCtx = paramDeclarationCtx.parameter_declarator();

                if (paramDeclaratorCtx == null) continue; // e.g. main(void)

                GLSLParser.Type_specifierContext paramTypeSpecCtx = paramDeclaratorCtx.type_specifier();
                String paramTypeName = paramTypeSpecCtx.getText();
                GLSLType paramType = symbols.getTypeByName(paramTypeName);

                if (paramType == null) {
                    debugLog("Failed to find type with name " + paramTypeName + ", using float instead");
                    paramType = symbols.getTypeByName("float");
                }

                String paramName = paramDeclaratorCtx.IDENTIFIER().getText();

                symbols.declareVar(paramName, paramType);

                paramTypes.add(paramType);
            }
        }

        symbols.declareFunction(name, type, paramTypes);

        depth++;
        super.visitFunction_definition(ctx);
        depth--;
        symbols.exitVarScope();
        return null;
    }

    private boolean isBuiltinOverride(GLSLParser.Function_prototypeContext ctx) {
        TerminalNode nameNode = ctx.IDENTIFIER();
        if (nameNode == null) return false;

        String name = ctx.IDENTIFIER().getText();
        if (name == null) return false;

        ArrayList<SymbolTable.FunctionRecord> builtinOverloads = SymbolTable.BUILTIN_FUNCTIONS.get(name);
        if (builtinOverloads == null) return false;

        for (SymbolTable.FunctionRecord overload : builtinOverloads) {
            String returnTypeName = getReturnType(ctx);
            if (returnTypeName == null || !matchesReturnType(returnTypeName, overload.returnType)) continue;

            List<String> params = extractParameterTypes(ctx);
            if (params == null || !matchesParameters(params, overload.parameterTypes)) continue;

            return true;
        }
        return false;
    }

    private String getReturnType(GLSLParser.Function_prototypeContext ctx) {
        var typeCtx = ctx.fully_specified_type();
        return (typeCtx != null) ? typeCtx.getText() : null;
    }

    private List<String> extractParameterTypes(GLSLParser.Function_prototypeContext ctx) {
        List<String> params = new ArrayList<>();
        if (ctx.function_parameters() != null) {
            for (GLSLParser.Parameter_declarationContext paramDeclarationCtx : ctx.function_parameters().parameter_declaration()) {
                GLSLParser.Parameter_declaratorContext paramDeclaratorCtx = paramDeclarationCtx.parameter_declarator();
                if (paramDeclaratorCtx == null) return null;
                GLSLParser.Type_specifierContext paramTypeSpecCtx = paramDeclaratorCtx.type_specifier();
                if (paramTypeSpecCtx == null) return null;
                params.add(paramTypeSpecCtx.getText());
            }
        }
        return params;
    }

    private boolean matchesReturnType(String actual, GLSLType expected) {
        return actual.equals(expected.getName());
//
//                || (expected.equals("genType") && GEN_TYPES.contains(actual))
//                || (expected.equals("mat") && MAT_TYPES.contains(actual));
    }

    private boolean matchesParameters(List<String> actual, List<GLSLType> expected) {
        if (actual.size() != expected.size()) return false;

        for (int i = 0; i < actual.size(); i++) {
            String a = actual.get(i);
            GLSLType e = expected.get(i);
            if (!a.equals(e.getName())) {
                return false;
            }
        }
        return true;
    }

    public Void visitFunction_call_parameters(GLSLParser.Function_call_parametersContext ctx) {
        debugLog("visitFunction_call_parameters " + ctx.getText());
        depth++;
        super.visitFunction_call_parameters(ctx);
        depth--;
        return null;
    }

    private String inferTypeName(ParserRuleContext exprCtx) {
        String text = exprCtx.getText();

        if (symbols.getVarType(text) != null) {
            return symbols.getVarType(text).getName();
        }
        if (text.matches("\\d+")) {
            return "int";
        }
        if (text.matches("\\d+\\.\\d*")) {
            return "float";
        }
        return "unknown";
    }

    // very rough and mostly incorrect estimate, but hopefully good enough
    private LinkedList<Boolean> resolveFunctionParamsContext(String funcName, List<String> actualParamTypeNames) {
        ArrayList<SymbolTable.FunctionRecord> overloads = symbols.getFunctionOverloads(funcName);

        LinkedList<Boolean> floatContextStack = new LinkedList<>();

        for (SymbolTable.FunctionRecord function : overloads) {
            ArrayList<GLSLType> expectedParameterTypes = function.parameterTypes;
            if (expectedParameterTypes.size() != actualParamTypeNames.size()) continue;

            boolean match = true;
            for (int i = 0; i < expectedParameterTypes.size(); i++) {
                if (!typesCompatible(actualParamTypeNames.get(i), expectedParameterTypes.get(i))) {
                    match = false;
                    break;
                }
            }

            if (match) {
                for (GLSLType expected : expectedParameterTypes) {
                    floatContextStack.add(expected.getName().equals("float"));
                }
            }
        }
        return floatContextStack;
    }

    private boolean typesCompatible(String actual, GLSLType expected) {
        return actual.equals(expected.getName())
                /*|| (expected.equals("genType") && GEN_TYPES.contains(actual))*/
                || (actual.equals("int") && expected.getName().equals("float")) // this will be handled by our context aware terminal visitor
                || actual.equals("unknown");
    }


    @Override
    public Void visitVariable_identifier(GLSLParser.Variable_identifierContext ctx) {
        debugLog("visitVariable_identifier " + ctx.getText());
        depth++;
        super.visitVariable_identifier(ctx);
        depth--;
        return null;

    }

    @Override
    public Void visitPrimary_expression(GLSLParser.Primary_expressionContext ctx) {
        debugLog("visitPrimary_expression " + ctx.getText() + " " + isParentInFloatContext(ctx));
        floatContext.put(ctx, isParentInFloatContext(ctx));
        depth++;
        super.visitPrimary_expression(ctx);
        depth--;
        return null;

    }

    public boolean isParentInFloatContext(ParseTree ctx) {
        ParseTree parent = ctx.getParent();
        if (parent == null) {
            debugLog("parent is null");
            return false;
        }

        Boolean isParentFloatContext = floatContext.get(ctx.getParent());
        if (isParentFloatContext == null) {
            debugLog("float context stack doesn't contain parent");
            return false;
        }
        return isParentFloatContext;
    }

    @Override
    public Void visitField_selection(GLSLParser.Field_selectionContext ctx) {
        debugLog("visitField_selection " + ctx.getText() + " " + isParentInFloatContext(ctx));
        floatContext.put(ctx, isParentInFloatContext(ctx));
        depth++;
        super.visitField_selection(ctx);
        depth--;
        return null;

    }

    @Override
    public Void visitFunction_call(GLSLParser.Function_callContext ctx) {
        debugLog("visitFunction_call " + ctx.getText());
        depth++;
        super.visitFunction_call(ctx);
        depth--;
        return null;

    }

    @Override
    public Void visitFunction_identifier(GLSLParser.Function_identifierContext ctx) {
        debugLog("visitFunction_identifier " + ctx.getText());
        depth++;
        super.visitFunction_identifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitUnary_expression(GLSLParser.Unary_expressionContext ctx) {
        debugLog("visitUnary_expression " + ctx.getText() + " " + isParentInFloatContext(ctx));
        floatContext.put(ctx, isParentInFloatContext(ctx));
        depth++;
        super.visitUnary_expression(ctx);
        depth--;
        return null;

    }

    @Override
    public Void visitUnary_operator(GLSLParser.Unary_operatorContext ctx) {
        debugLog("visitUnary_operator " + ctx.getText());
        depth++;
        super.visitUnary_operator(ctx);
        depth--;
        return null;

    }

    @Override
    public Void visitAssignment_operator(GLSLParser.Assignment_operatorContext ctx) {
        debugLog("visitAssignment_operator " + ctx.getText());
        depth++;
        super.visitAssignment_operator(ctx);
        depth--;
        return null;

    }

    @Override
    public Void visitBinary_expression(GLSLParser.Binary_expressionContext ctx) {
        debugLog("visitBinary_expression " + ctx.getText() + " " + isParentInFloatContext(ctx));
        floatContext.put(ctx, isParentInFloatContext(ctx));
        depth++;
        super.visitBinary_expression(ctx);
        depth--;
        return null;

    }

    @Override
    public Void visitExpression(GLSLParser.ExpressionContext ctx) {
        debugLog("visitExpression " + ctx.getText() + " " + isParentInFloatContext(ctx));
        floatContext.put(ctx, isParentInFloatContext(ctx));
        depth++;
        super.visitExpression(ctx);
        depth--;
        return null;

    }

    @Override
    public Void visitConstant_expression(GLSLParser.Constant_expressionContext ctx) {
        debugLog("visitConstant_expression " + ctx.getText() + " " + isParentInFloatContext(ctx));
        floatContext.put(ctx, isParentInFloatContext(ctx));
        depth++;
        super.visitConstant_expression(ctx);
        depth--;
        return null;

    }

    @Override
    public Void visitDeclaration(GLSLParser.DeclarationContext ctx) {
        debugLog("visitDeclaration " + ctx.getText());
        GLSLParser.Function_prototypeContext funcProtoCtx = ctx.function_prototype();
        if (funcProtoCtx != null) {
            if (isBuiltinOverride(funcProtoCtx)) {
                // remove function declaration
                Token startToken = ctx.getStart();
                Token stopToken = ctx.getStop();
                for (int i = startToken.getTokenIndex(); i <= stopToken.getTokenIndex(); i++) {
                    replacements.put(i, "");
                }
                return null;
            }
        }
        depth++;
        super.visitDeclaration(ctx);
        depth--;
        return null;

    }

    @Override
    public Void visitIdentifier_list(GLSLParser.Identifier_listContext ctx) {
        debugLog("visitIdentifier_list " + ctx.getText());
        depth++;
        super.visitIdentifier_list(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitFunction_parameters(GLSLParser.Function_parametersContext ctx) {
        debugLog("visitFunction_parameters " + ctx.getText());
        depth++;
        super.visitFunction_parameters(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitParameter_declarator(GLSLParser.Parameter_declaratorContext ctx) {
        debugLog("visitParameter_declarator " + ctx.getText());
        depth++;
        super.visitParameter_declarator(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitParameter_declaration(GLSLParser.Parameter_declarationContext ctx) {
        debugLog("visitParameter_declaration " + ctx.getText());
        depth++;
        super.visitParameter_declaration(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitParameter_type_specifier(GLSLParser.Parameter_type_specifierContext ctx) {
        debugLog("visitParameter_type_specifier " + ctx.getText());
        depth++;
        super.visitParameter_type_specifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitInit_declarator_list(GLSLParser.Init_declarator_listContext ctx) {
        debugLog("visitInit_declarator_list " + ctx.getText());
        depth++;
        super.visitInit_declarator_list(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitTypeless_declaration(GLSLParser.Typeless_declarationContext ctx) {
        debugLog("visitTypeless_declaration " + ctx.getText() + " " + isParentInFloatContext(ctx));
        floatContext.put(ctx, isParentInFloatContext(ctx));
        depth++;
        super.visitTypeless_declaration(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitFully_specified_type(GLSLParser.Fully_specified_typeContext ctx) {
        debugLog("visitFully_specified_type " + ctx.getText());
        depth++;
        super.visitFully_specified_type(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitInvariant_qualifier(GLSLParser.Invariant_qualifierContext ctx) {
        debugLog("visitInvariant_qualifier " + ctx.getText());
        depth++;
        super.visitInvariant_qualifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitInterpolation_qualifier(GLSLParser.Interpolation_qualifierContext ctx) {
        debugLog("visitInterpolation_qualifier " + ctx.getText());
        depth++;
        super.visitInterpolation_qualifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitLayout_qualifier(GLSLParser.Layout_qualifierContext ctx) {
        debugLog("visitLayout_qualifier " + ctx.getText());
        depth++;
        super.visitLayout_qualifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitLayout_qualifier_id_list(GLSLParser.Layout_qualifier_id_listContext ctx) {
        debugLog("visitLayout_qualifier_id_list " + ctx.getText());
        depth++;
        super.visitLayout_qualifier_id_list(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitLayout_qualifier_id(GLSLParser.Layout_qualifier_idContext ctx) {
        debugLog("visitLayout_qualifier_id " + ctx.getText());
        depth++;
        super.visitLayout_qualifier_id(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitPrecise_qualifier(GLSLParser.Precise_qualifierContext ctx) {
        debugLog("visitPrecise_qualifier " + ctx.getText());
        depth++;
        super.visitPrecise_qualifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitType_qualifier(GLSLParser.Type_qualifierContext ctx) {
        debugLog("visitType_qualifier " + ctx.getText());
        depth++;
        super.visitType_qualifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitSingle_type_qualifier(GLSLParser.Single_type_qualifierContext ctx) {
        debugLog("visitSingle_type_qualifier " + ctx.getText());
        depth++;
        super.visitSingle_type_qualifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitStorage_qualifier(GLSLParser.Storage_qualifierContext ctx) {
        debugLog("visitStorage_qualifier " + ctx.getText());
        depth++;
        super.visitStorage_qualifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitType_name_list(GLSLParser.Type_name_listContext ctx) {
        debugLog("visitType_name_list " + ctx.getText());
        depth++;
        super.visitType_name_list(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitType_name(GLSLParser.Type_nameContext ctx) {
        debugLog("visitType_name " + ctx.getText());
        depth++;
        super.visitType_name(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitType_specifier(GLSLParser.Type_specifierContext ctx) {
        debugLog("visitType_specifier " + ctx.getText());
        depth++;
        super.visitType_specifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitArray_specifier(GLSLParser.Array_specifierContext ctx) {
        debugLog("visitArray_specifier " + ctx.getText());
        depth++;
        super.visitArray_specifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitDimension(GLSLParser.DimensionContext ctx) {
        debugLog("visitDimension " + ctx.getText());
        depth++;
        super.visitDimension(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitType_specifier_nonarray(GLSLParser.Type_specifier_nonarrayContext ctx) {
        debugLog("visitType_specifier_nonarray " + ctx.getText());
        depth++;
        super.visitType_specifier_nonarray(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitPrecision_qualifier(GLSLParser.Precision_qualifierContext ctx) {
        debugLog("visitPrecision_qualifier " + ctx.getText());
        depth++;
        super.visitPrecision_qualifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitStruct_specifier(GLSLParser.Struct_specifierContext ctx) {
        debugLog("visitStruct_specifier " + ctx.getText());
        depth++;
        super.visitStruct_specifier(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitStruct_declaration_list(GLSLParser.Struct_declaration_listContext ctx) {
        debugLog("visitStruct_declaration_list " + ctx.getText());
        depth++;
        super.visitStruct_declaration_list(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitStruct_declaration(GLSLParser.Struct_declarationContext ctx) {
        debugLog("visitStruct_declaration " + ctx.getText());
        depth++;
        super.visitStruct_declaration(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitStruct_declarator_list(GLSLParser.Struct_declarator_listContext ctx) {
        debugLog("visitStruct_declarator_list " + ctx.getText());
        depth++;
        super.visitStruct_declarator_list(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitStruct_declarator(GLSLParser.Struct_declaratorContext ctx) {
        debugLog("visitStruct_declarator " + ctx.getText());
        depth++;
        super.visitStruct_declarator(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitInitializer(GLSLParser.InitializerContext ctx) {
        debugLog("visitInitializer " + ctx.getText() + " " + isParentInFloatContext(ctx));
        floatContext.put(ctx, isParentInFloatContext(ctx));
        depth++;
        super.visitInitializer(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitInitializer_list(GLSLParser.Initializer_listContext ctx) {
        debugLog("visitInitializer_list " + ctx.getText());
        depth++;
        super.visitInitializer_list(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitDeclaration_statement(GLSLParser.Declaration_statementContext ctx) {
        debugLog("visitDeclaration_statement " + ctx.getText());
        depth++;
        super.visitDeclaration_statement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitStatement(GLSLParser.StatementContext ctx) {
        debugLog("visitStatement " + ctx.getText());
        depth++;
        super.visitStatement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitSimple_statement(GLSLParser.Simple_statementContext ctx) {
        debugLog("visitSimple_statement " + ctx.getText());
        depth++;
        super.visitSimple_statement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitCompound_statement(GLSLParser.Compound_statementContext ctx) {
        debugLog("visitCompound_statement " + ctx.getText());
        symbols.enterNewVarScope();
        depth++;
        super.visitCompound_statement(ctx);
        depth--;
        symbols.exitVarScope();
        return null;
    }

    @Override
    public Void visitStatement_no_new_scope(GLSLParser.Statement_no_new_scopeContext ctx) {
        debugLog("visitStatement_no_new_scope " + ctx.getText());
        depth++;
        super.visitStatement_no_new_scope(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitCompound_statement_no_new_scope(GLSLParser.Compound_statement_no_new_scopeContext ctx) {
        debugLog("visitCompound_statement_no_new_scope " + ctx.getText());
        depth++;
        super.visitCompound_statement_no_new_scope(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitStatement_list(GLSLParser.Statement_listContext ctx) {
        debugLog("visitStatement_list " + ctx.getText());
        depth++;
        super.visitStatement_list(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitExpression_statement(GLSLParser.Expression_statementContext ctx) {
        debugLog("visitExpression_statement " + ctx.getText());
        depth++;
        super.visitExpression_statement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitSelection_statement(GLSLParser.Selection_statementContext ctx) {
        debugLog("visitSelection_statement " + ctx.getText());
        depth++;
        super.visitSelection_statement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitSelection_rest_statement(GLSLParser.Selection_rest_statementContext ctx) {
        debugLog("visitSelection_rest_statement " + ctx.getText());
        depth++;
        super.visitSelection_rest_statement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitCondition(GLSLParser.ConditionContext ctx) {
        debugLog("visitCondition " + ctx.getText());
        depth++;
        super.visitCondition(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitSwitch_statement(GLSLParser.Switch_statementContext ctx) {
        debugLog("visitSwitch_statement " + ctx.getText());
        depth++;
        super.visitSwitch_statement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitCase_label(GLSLParser.Case_labelContext ctx) {
        debugLog("visitCase_label " + ctx.getText());
        depth++;
        super.visitCase_label(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitIteration_statement(GLSLParser.Iteration_statementContext ctx) {
        debugLog("visitIteration_statement " + ctx.getText());
        depth++;
        super.visitIteration_statement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitFor_init_statement(GLSLParser.For_init_statementContext ctx) {
        debugLog("visitFor_init_statement " + ctx.getText());
        depth++;
        super.visitFor_init_statement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitFor_rest_statement(GLSLParser.For_rest_statementContext ctx) {
        debugLog("visitFor_rest_statement " + ctx.getText());
        depth++;
        super.visitFor_rest_statement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitJump_statement(GLSLParser.Jump_statementContext ctx) {
        debugLog("visitJump_statement " + ctx.getText());
        depth++;
        super.visitJump_statement(ctx);
        depth--;
        return null;
    }

    @Override
    public Void visitExternal_declaration(GLSLParser.External_declarationContext ctx) {
        debugLog("visitExternal_declaration " + ctx.getText());
        depth++;
        super.visitExternal_declaration(ctx);
        depth--;
        return null;
    }

    // Symbol table for vars and functions
    public static class SymbolTable {
        private static final List<String> GEN_TYPES = List.of("float", "vec2", "vec3", "vec4");
        private static final List<String> MAT_TYPES = List.of("mat2", "mat3", "mat4");
        private static final Map<String, ArrayList<FunctionRecord>> BUILTIN_FUNCTIONS = new HashMap<>();
        private static final Map<String, GLSLType> BUILTIN_TYPES = new HashMap<>();

        private final Deque<Map<String, GLSLType>> vars = new ArrayDeque<>(List.of(new HashMap<>()));
        private final Map<String, ArrayList<FunctionRecord>> functions = new HashMap<>();
        private final Map<String, GLSLType> types = new HashMap<>();

        static {
            GLSLType voidType = new GLSLType.PrimitiveType("void");
            BUILTIN_TYPES.put(voidType.getName(), voidType);

            GLSLType boolType = new GLSLType.PrimitiveType("bool");
            BUILTIN_TYPES.put(boolType.getName(), boolType);

            GLSLType intType = new GLSLType.PrimitiveType("int");
            BUILTIN_TYPES.put(intType.getName(), intType);

            GLSLType floatType = new GLSLType.PrimitiveType("float");
            BUILTIN_TYPES.put(floatType.getName(), floatType);

            GLSLType.VectorType vec2 = GLSLType.VectorType.from(floatType, 2);
            BUILTIN_TYPES.put(vec2.getName(), vec2);

            GLSLType.VectorType vec3 = GLSLType.VectorType.from(floatType, 3);
            BUILTIN_TYPES.put(vec3.getName(), vec3);

            GLSLType.VectorType vec4 = GLSLType.VectorType.from(floatType, 4);
            BUILTIN_TYPES.put(vec4.getName(), vec4);

            GLSLType.VectorType bvec2 = GLSLType.VectorType.from(boolType, 2);
            BUILTIN_TYPES.put(bvec2.getName(), bvec2);

            GLSLType.VectorType bvec3 = GLSLType.VectorType.from(boolType, 3);
            BUILTIN_TYPES.put(bvec3.getName(), bvec3);

            GLSLType.VectorType bvec4 = GLSLType.VectorType.from(boolType, 4);
            BUILTIN_TYPES.put(bvec4.getName(), bvec4);

            GLSLType.VectorType ivec2 = GLSLType.VectorType.from(intType, 2);
            BUILTIN_TYPES.put(ivec2.getName(), ivec2);

            GLSLType.VectorType ivec3 = GLSLType.VectorType.from(intType, 3);
            BUILTIN_TYPES.put(ivec3.getName(), ivec3);

            GLSLType.VectorType ivec4 = GLSLType.VectorType.from(intType, 4);
            BUILTIN_TYPES.put(ivec4.getName(), ivec4);

            GLSLType.PrimitiveType mat2 = new GLSLType.PrimitiveType("mat2");
            BUILTIN_TYPES.put(mat2.getName(), mat2);

            GLSLType.PrimitiveType mat3 = new GLSLType.PrimitiveType("mat3");
            BUILTIN_TYPES.put(mat3.getName(), mat3);

            GLSLType.PrimitiveType mat4 = new GLSLType.PrimitiveType("mat4");
            BUILTIN_TYPES.put(mat4.getName(), mat4);

            GLSLType.PrimitiveType sampler2D = new GLSLType.PrimitiveType("sampler2D");
            BUILTIN_TYPES.put(sampler2D.getName(), sampler2D);

            GLSLType.PrimitiveType samplerCube = new GLSLType.PrimitiveType("samplerCube");
            BUILTIN_TYPES.put(samplerCube.getName(), samplerCube);

            try (InputStream in = GLSLCoreToESVisitor.class.getClassLoader().getResourceAsStream("GLSL_ES_2_BUILTINS");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

                // matches lines like returnType name(paramType paramName, ...)
                Pattern pattern = Pattern.compile("(\\w+)\\s+(\\w+)\\s*\\(([^)]*)\\)");

                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String returnTypeName = matcher.group(1);
                        String name = matcher.group(2);
                        String params = matcher.group(3).trim();

                        ArrayList<FunctionRecord> overloads = new ArrayList<>();

                        if (returnTypeName.equals("genType") || params.contains("genType")) {
                            for (String genTypeName : GEN_TYPES) {
                                GLSLType genType = BUILTIN_TYPES.get(genTypeName);

                                GLSLType returnType = returnTypeName.equals("genType") ? genType : BUILTIN_TYPES.get(returnTypeName);

                                ArrayList<GLSLType> paramTypes = new ArrayList<>();
                                for (String param : params.split(",")) {
                                    String paramTypeName = param.trim().split("\\s+")[0];
                                    if (paramTypeName.equals("genType")) paramTypes.add(genType);
                                    else paramTypes.add(BUILTIN_TYPES.get(paramTypeName));
                                }
                                FunctionRecord function = new FunctionRecord(name, returnType, paramTypes);
                                overloads.add(function);
                            }
                        } else if (returnTypeName.equals("matType") || params.contains("matType")) {
                            for (String matTypeName : MAT_TYPES) {
                                GLSLType matType = BUILTIN_TYPES.get(matTypeName);

                                GLSLType returnType = returnTypeName.equals("matType") ? matType : BUILTIN_TYPES.get(returnTypeName);

                                ArrayList<GLSLType> paramTypes = new ArrayList<>();
                                for (String param : params.split(",")) {
                                    String paramTypeName = param.trim().split("\\s+")[0];
                                    if (paramTypeName.equals("matType")) paramTypes.add(matType);
                                    else paramTypes.add(BUILTIN_TYPES.get(paramTypeName));
                                }
                                FunctionRecord function = new FunctionRecord(name, returnType, paramTypes);
                                overloads.add(function);
                            }
                        } else {
                            GLSLType returnType = BUILTIN_TYPES.get(returnTypeName);
                            ArrayList<GLSLType> paramTypes = new ArrayList<>();
                            for (String param : params.split(",")) {
                                String paramTypeName = param.trim().split("\\s+")[0];
                                paramTypes.add(BUILTIN_TYPES.get(paramTypeName));
                            }
                            FunctionRecord function = new FunctionRecord(name, returnType, paramTypes);
                            overloads.add(function);
                        }

                        BUILTIN_FUNCTIONS.put(name, overloads);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to load built-in GLSL functions", e);
            }
        }


        public void declareFunction(String name, GLSLType returnType, ArrayList<GLSLType> paramTypes) {
            ArrayList<FunctionRecord> overloads = functions.computeIfAbsent(name, k -> new ArrayList<>());
            if (paramTypes.contains(null))
                throw new IllegalArgumentException("paramTypes must not contain null");
            overloads.add(new FunctionRecord(name, returnType, paramTypes));
        }

        public ArrayList<FunctionRecord> getFunctionOverloads(String name) {
            ArrayList<FunctionRecord> overloads = new ArrayList<>();

            ArrayList<FunctionRecord> userDefined = functions.get(name);
            if (userDefined != null) overloads.addAll(userDefined);

            ArrayList<FunctionRecord> builtin = BUILTIN_FUNCTIONS.get(name);
            if (builtin != null) overloads.addAll(builtin);

            return overloads;
        }

        public void enterNewVarScope() {
            vars.push(new HashMap<>());
        }

        public void exitVarScope() {
            vars.pop();
        }

        public void declareVar(String name, GLSLType type) {
            vars.peek().put(name, type);
        }

        public GLSLType getVarType(String name) {
            for (Map<String, GLSLType> varScope: vars) {
                if (varScope.containsKey(name)) return varScope.get(name);
            }
            return null;
        }

        public void declareStruct(String name, ArrayList<String> fieldNames, ArrayList<GLSLType> fieldTypes) {
            GLSLType struct = new GLSLType.StructType(name, fieldNames.toArray(new String[0]), fieldTypes.toArray(new GLSLType[0]));
            types.put(name, struct);
        }

        public GLSLType getTypeByName(String name) {
            GLSLType type = BUILTIN_TYPES.get(name);
            if (type == null) {
                type = types.get(name);
            }
            return type;
        }

        public record FunctionRecord(String name, GLSLType returnType, ArrayList<GLSLType> parameterTypes) {
        }
    }
}
