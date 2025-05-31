package com.zomdroid.agent.glsl;

import com.zomdroid.agent.glsl.antlr.GLSLLexer;
import com.zomdroid.agent.glsl.antlr.GLSLParser;
import com.zomdroid.agent.glsl.antlr.GLSLParserBaseVisitor;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayDeque;
import java.util.Deque;


/**
 * A bottom-up visitor that analyzes GLSL expressions to determine whether an expression
 * should be treated as floating-point based on the types of its child expressions.
 * This is useful in cases where the type context cannot be reliably inherited from parent
 * constructs — for example in
 * <pre>
 *   {@code if (1 > b)}
 * </pre>
 * the type of the literal <code>1</code> must be inferred from the type of
 * <code>b</code> or from the nature of the binary operation itself.
 * <p>
 * This visitor does not perform transformations or build replacement maps. Instead, it returns
 * a boolean result indicating whether a given expression resolves to a float context (i.e., whether
 * any subexpression is float-typed).
 * Designed to complement {@link GLSLCoreToESVisitor}, which uses a top-down approach to
 * propagate type context from parent nodes.
 */
public class GLSLBottomUpContextResolver extends GLSLParserBaseVisitor<Boolean> {
    private final GLSLCoreToESVisitor.SymbolTable symbolTable;
    private final Deque<GLSLType> typeStack = new ArrayDeque<>();

    private int depth = 0;

    private void debugLog(String message) {
        String indent = "\t".repeat(depth);
        //System.out.println(indent + message);
    }

    public GLSLBottomUpContextResolver(GLSLCoreToESVisitor.SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    public Boolean aggregateResult(Boolean aggregate, Boolean nextResult) {
        return (aggregate != null && aggregate) || (nextResult != null && nextResult); // one float in a context is enough
    }

    @Override
    public Boolean defaultResult() {
        return false;
    }

    @Override
    public Boolean visitTerminal(TerminalNode node) {
        Token tok = node.getSymbol();

        debugLog("visitTerminal " + tok.getText());

        if (tok.getType() == GLSLLexer.FLOATCONSTANT) {
            return true;
        }

        GLSLType typeOnStack = typeStack.peek();
        if (typeOnStack != null) {
            return typeOnStack.getName().equals("float");
        }

        return false;
    }

    @Override
    public Boolean visitVariable_identifier(GLSLParser.Variable_identifierContext ctx) {
        debugLog("visitVariable_identifier " + ctx.getText());

        String varName = ctx.getText();
        if (ctx.parent instanceof GLSLParser.Primary_expressionContext) {
            GLSLType varType = symbolTable.getVarType(varName);
            if (varType == null) {
                if (symbolTable.getFunctionOverloads(varName) == null)
                    debugLog("Identifier " + varName + " is neither variable nor function name");
            } else {
                typeStack.push(varType);
            }
        } else if (ctx.parent instanceof GLSLParser.Field_selectionContext) {
            GLSLType parentType =  typeStack.peek();
            if (!(parentType instanceof GLSLType.StructType)) {
                debugLog("Unexpected parent type " + parentType);
            } else {
                GLSLType varType = ((GLSLType.StructType) parentType).getFieldType(varName);
                if (varType == null) {
                    debugLog("Unknown struct field " + varName + " for struct " + parentType.getName());
                } else {
                    typeStack.push(varType);
                }
            }
        }

        this.depth++;
        Boolean result = super.visitVariable_identifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitPrimary_expression(GLSLParser.Primary_expressionContext ctx) {
        debugLog("visitPrimary_expression " + ctx.getText());
        this.depth++;
        Boolean result = super.visitPrimary_expression(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitPostfix_expression(GLSLParser.Postfix_expressionContext ctx) {
        debugLog("visitPostfix_expression " + ctx.getText());
        this.depth++;
        Boolean result = super.visitPostfix_expression(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitField_selection(GLSLParser.Field_selectionContext ctx) {
        debugLog("visitField_selection " + ctx.getText());
        this.depth++;
        Boolean result = super.visitField_selection(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitInteger_expression(GLSLParser.Integer_expressionContext ctx) {
        debugLog("visitInteger_expression " + ctx.getText());
        this.depth++;
        Boolean result = super.visitInteger_expression(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitFunction_call(GLSLParser.Function_callContext ctx) {
        debugLog("visitFunction_call " + ctx.getText());
        this.depth++;
        Boolean result = super.visitFunction_call(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitFunction_identifier(GLSLParser.Function_identifierContext ctx) {
        debugLog("visitFunction_identifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitFunction_identifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitFunction_call_parameters(GLSLParser.Function_call_parametersContext ctx) {
        debugLog("visitFunction_call_parameters " + ctx.getText());
        this.depth++;
        Boolean result = super.visitFunction_call_parameters(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitUnary_expression(GLSLParser.Unary_expressionContext ctx) {
        debugLog("visitUnary_expression " + ctx.getText());
        this.depth++;
        Boolean result = super.visitUnary_expression(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitUnary_operator(GLSLParser.Unary_operatorContext ctx) {
        debugLog("visitUnary_operator " + ctx.getText());
        this.depth++;
        Boolean result = super.visitUnary_operator(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitAssignment_expression(GLSLParser.Assignment_expressionContext ctx) {
        debugLog("visitAssignment_expression " + ctx.getText());
        this.depth++;
        Boolean result = super.visitAssignment_expression(ctx);
        this.depth--;
        typeStack.clear();
        return result;
    }

    @Override
    public Boolean visitAssignment_operator(GLSLParser.Assignment_operatorContext ctx) {
        debugLog("visitAssignment_operator " + ctx.getText());
        this.depth++;
        Boolean result = super.visitAssignment_operator(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitBinary_expression(GLSLParser.Binary_expressionContext ctx) {
        debugLog("visitBinary_expression " + ctx.getText());
        this.depth++;
        Boolean result = super.visitBinary_expression(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitExpression(GLSLParser.ExpressionContext ctx) {
        debugLog("visitExpression " + ctx.getText());
        this.depth++;
        Boolean result = super.visitExpression(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitConstant_expression(GLSLParser.Constant_expressionContext ctx) {
        debugLog("visitConstant_expression " + ctx.getText());
        this.depth++;
        Boolean result = super.visitConstant_expression(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitDeclaration(GLSLParser.DeclarationContext ctx) {
        debugLog("visitDeclaration " + ctx.getText());
        this.depth++;
        Boolean result = super.visitDeclaration(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitIdentifier_list(GLSLParser.Identifier_listContext ctx) {
        debugLog("visitIdentifier_list " + ctx.getText());
        this.depth++;
        Boolean result = super.visitIdentifier_list(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitFunction_prototype(GLSLParser.Function_prototypeContext ctx) {
        debugLog("visitFunction_prototype " + ctx.getText());
        this.depth++;
        Boolean result = super.visitFunction_prototype(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitFunction_parameters(GLSLParser.Function_parametersContext ctx) {
        debugLog("visitFunction_parameters " + ctx.getText());
        this.depth++;
        Boolean result = super.visitFunction_parameters(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitParameter_declarator(GLSLParser.Parameter_declaratorContext ctx) {
        debugLog("visitParameter_declarator " + ctx.getText());
        this.depth++;
        Boolean result = super.visitParameter_declarator(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitParameter_declaration(GLSLParser.Parameter_declarationContext ctx) {
        debugLog("visitParameter_declaration " + ctx.getText());
        this.depth++;
        Boolean result = super.visitParameter_declaration(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitParameter_type_specifier(GLSLParser.Parameter_type_specifierContext ctx) {
        debugLog("visitParameter_type_specifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitParameter_type_specifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitInit_declarator_list(GLSLParser.Init_declarator_listContext ctx) {
        debugLog("visitInit_declarator_list " + ctx.getText());
        this.depth++;
        Boolean result = super.visitInit_declarator_list(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitSingle_declaration(GLSLParser.Single_declarationContext ctx) {
        debugLog("visitSingle_declaration " + ctx.getText());
        this.depth++;
        Boolean result = super.visitSingle_declaration(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitTypeless_declaration(GLSLParser.Typeless_declarationContext ctx) {
        debugLog("visitTypeless_declaration " + ctx.getText());
        this.depth++;
        Boolean result = super.visitTypeless_declaration(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitFully_specified_type(GLSLParser.Fully_specified_typeContext ctx) {
        debugLog("visitFully_specified_type " + ctx.getText());
        this.depth++;
        Boolean result = super.visitFully_specified_type(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitInvariant_qualifier(GLSLParser.Invariant_qualifierContext ctx) {
        debugLog("visitInvariant_qualifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitInvariant_qualifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitInterpolation_qualifier(GLSLParser.Interpolation_qualifierContext ctx) {
        debugLog("visitInterpolation_qualifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitInterpolation_qualifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitLayout_qualifier(GLSLParser.Layout_qualifierContext ctx) {
        debugLog("visitLayout_qualifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitLayout_qualifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitLayout_qualifier_id_list(GLSLParser.Layout_qualifier_id_listContext ctx) {
        debugLog("visitLayout_qualifier_id_list " + ctx.getText());
        this.depth++;
        Boolean result = super.visitLayout_qualifier_id_list(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitLayout_qualifier_id(GLSLParser.Layout_qualifier_idContext ctx) {
        debugLog("visitLayout_qualifier_id " + ctx.getText());
        this.depth++;
        Boolean result = super.visitLayout_qualifier_id(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitPrecise_qualifier(GLSLParser.Precise_qualifierContext ctx) {
        debugLog("visitPrecise_qualifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitPrecise_qualifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitType_qualifier(GLSLParser.Type_qualifierContext ctx) {
        debugLog("visitType_qualifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitType_qualifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitSingle_type_qualifier(GLSLParser.Single_type_qualifierContext ctx) {
        debugLog("visitSingle_type_qualifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitSingle_type_qualifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitStorage_qualifier(GLSLParser.Storage_qualifierContext ctx) {
        debugLog("visitStorage_qualifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitStorage_qualifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitType_name_list(GLSLParser.Type_name_listContext ctx) {
        debugLog("visitType_name_list " + ctx.getText());
        this.depth++;
        Boolean result = super.visitType_name_list(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitType_name(GLSLParser.Type_nameContext ctx) {
        debugLog("visitType_name " + ctx.getText());
        this.depth++;
        Boolean result = super.visitType_name(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitType_specifier(GLSLParser.Type_specifierContext ctx) {
        debugLog("visitType_specifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitType_specifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitArray_specifier(GLSLParser.Array_specifierContext ctx) {
        debugLog("visitArray_specifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitArray_specifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitDimension(GLSLParser.DimensionContext ctx) {
        debugLog("visitDimension " + ctx.getText());
        this.depth++;
        Boolean result = super.visitDimension(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitType_specifier_nonarray(GLSLParser.Type_specifier_nonarrayContext ctx) {
        debugLog("visitType_specifier_nonarray " + ctx.getText());
        this.depth++;
        Boolean result = super.visitType_specifier_nonarray(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitPrecision_qualifier(GLSLParser.Precision_qualifierContext ctx) {
        debugLog("visitPrecision_qualifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitPrecision_qualifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitStruct_specifier(GLSLParser.Struct_specifierContext ctx) {
        debugLog("visitStruct_specifier " + ctx.getText());
        this.depth++;
        Boolean result = super.visitStruct_specifier(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitStruct_declaration_list(GLSLParser.Struct_declaration_listContext ctx) {
        debugLog("visitStruct_declaration_list " + ctx.getText());
        this.depth++;
        Boolean result = super.visitStruct_declaration_list(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitStruct_declaration(GLSLParser.Struct_declarationContext ctx) {
        debugLog("visitStruct_declaration " + ctx.getText());
        this.depth++;
        Boolean result = super.visitStruct_declaration(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitStruct_declarator_list(GLSLParser.Struct_declarator_listContext ctx) {
        debugLog("visitStruct_declarator_list " + ctx.getText());
        this.depth++;
        Boolean result = super.visitStruct_declarator_list(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitStruct_declarator(GLSLParser.Struct_declaratorContext ctx) {
        debugLog("visitStruct_declarator " + ctx.getText());
        this.depth++;
        Boolean result = super.visitStruct_declarator(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitInitializer(GLSLParser.InitializerContext ctx) {
        debugLog("visitInitializer " + ctx.getText());
        this.depth++;
        Boolean result = super.visitInitializer(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitInitializer_list(GLSLParser.Initializer_listContext ctx) {
        debugLog("visitInitializer_list " + ctx.getText());
        this.depth++;
        Boolean result = super.visitInitializer_list(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitDeclaration_statement(GLSLParser.Declaration_statementContext ctx) {
        debugLog("visitDeclaration_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitDeclaration_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitStatement(GLSLParser.StatementContext ctx) {
        debugLog("visitStatement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitStatement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitSimple_statement(GLSLParser.Simple_statementContext ctx) {
        debugLog("visitSimple_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitSimple_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitCompound_statement(GLSLParser.Compound_statementContext ctx) {
        debugLog("visitCompound_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitCompound_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitStatement_no_new_scope(GLSLParser.Statement_no_new_scopeContext ctx) {
        debugLog("visitStatement_no_new_scope " + ctx.getText());
        this.depth++;
        Boolean result = super.visitStatement_no_new_scope(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitCompound_statement_no_new_scope(GLSLParser.Compound_statement_no_new_scopeContext ctx) {
        debugLog("visitCompound_statement_no_new_scope " + ctx.getText());
        this.depth++;
        Boolean result = super.visitCompound_statement_no_new_scope(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitStatement_list(GLSLParser.Statement_listContext ctx) {
        debugLog("visitStatement_list " + ctx.getText());
        this.depth++;
        Boolean result = super.visitStatement_list(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitExpression_statement(GLSLParser.Expression_statementContext ctx) {
        debugLog("visitExpression_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitExpression_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitSelection_statement(GLSLParser.Selection_statementContext ctx) {
        debugLog("visitSelection_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitSelection_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitSelection_rest_statement(GLSLParser.Selection_rest_statementContext ctx) {
        debugLog("visitSelection_rest_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitSelection_rest_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitCondition(GLSLParser.ConditionContext ctx) {
        debugLog("visitCondition " + ctx.getText());
        this.depth++;
        Boolean result = super.visitCondition(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitSwitch_statement(GLSLParser.Switch_statementContext ctx) {
        debugLog("visitSwitch_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitSwitch_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitCase_label(GLSLParser.Case_labelContext ctx) {
        debugLog("visitCase_label " + ctx.getText());
        this.depth++;
        Boolean result = super.visitCase_label(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitIteration_statement(GLSLParser.Iteration_statementContext ctx) {
        debugLog("visitIteration_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitIteration_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitFor_init_statement(GLSLParser.For_init_statementContext ctx) {
        debugLog("visitFor_init_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitFor_init_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitFor_rest_statement(GLSLParser.For_rest_statementContext ctx) {
        debugLog("visitFor_rest_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitFor_rest_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitJump_statement(GLSLParser.Jump_statementContext ctx) {
        debugLog("visitJump_statement " + ctx.getText());
        this.depth++;
        Boolean result = super.visitJump_statement(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitExternal_declaration(GLSLParser.External_declarationContext ctx) {
        debugLog("visitExternal_declaration " + ctx.getText());
        this.depth++;
        Boolean result = super.visitExternal_declaration(ctx);
        this.depth--;
        return result;
    }

    @Override
    public Boolean visitFunction_definition(GLSLParser.Function_definitionContext ctx) {
        debugLog("visitFunction_definition " + ctx.getText());
        this.depth++;
        Boolean result = super.visitFunction_definition(ctx);
        this.depth--;
        return result;
    }
}
