package com.zomdroid.agent.glsl.antlr;
// Generated from GLSLParser.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link GLSLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface GLSLParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link GLSLParser#translation_unit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTranslation_unit(GLSLParser.Translation_unitContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#variable_identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_identifier(GLSLParser.Variable_identifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#primary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary_expression(GLSLParser.Primary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#postfix_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfix_expression(GLSLParser.Postfix_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#field_selection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField_selection(GLSLParser.Field_selectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#integer_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInteger_expression(GLSLParser.Integer_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call(GLSLParser.Function_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#function_identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_identifier(GLSLParser.Function_identifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#function_call_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call_parameters(GLSLParser.Function_call_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#unary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_expression(GLSLParser.Unary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#unary_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_operator(GLSLParser.Unary_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#assignment_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_expression(GLSLParser.Assignment_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#assignment_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_operator(GLSLParser.Assignment_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#binary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinary_expression(GLSLParser.Binary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(GLSLParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#constant_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant_expression(GLSLParser.Constant_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration(GLSLParser.DeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#identifier_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier_list(GLSLParser.Identifier_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#function_prototype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_prototype(GLSLParser.Function_prototypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#function_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_parameters(GLSLParser.Function_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#parameter_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_declarator(GLSLParser.Parameter_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#parameter_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_declaration(GLSLParser.Parameter_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#parameter_type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_type_specifier(GLSLParser.Parameter_type_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#init_declarator_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInit_declarator_list(GLSLParser.Init_declarator_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#single_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_declaration(GLSLParser.Single_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#typeless_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeless_declaration(GLSLParser.Typeless_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#fully_specified_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFully_specified_type(GLSLParser.Fully_specified_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#invariant_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvariant_qualifier(GLSLParser.Invariant_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#interpolation_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolation_qualifier(GLSLParser.Interpolation_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#layout_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLayout_qualifier(GLSLParser.Layout_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#layout_qualifier_id_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLayout_qualifier_id_list(GLSLParser.Layout_qualifier_id_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#layout_qualifier_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLayout_qualifier_id(GLSLParser.Layout_qualifier_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#precise_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrecise_qualifier(GLSLParser.Precise_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#type_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_qualifier(GLSLParser.Type_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#single_type_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_type_qualifier(GLSLParser.Single_type_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#storage_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_qualifier(GLSLParser.Storage_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#type_name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_name_list(GLSLParser.Type_name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_name(GLSLParser.Type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_specifier(GLSLParser.Type_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#array_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_specifier(GLSLParser.Array_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#dimension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDimension(GLSLParser.DimensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#type_specifier_nonarray}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_specifier_nonarray(GLSLParser.Type_specifier_nonarrayContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#precision_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrecision_qualifier(GLSLParser.Precision_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#struct_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_specifier(GLSLParser.Struct_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#struct_declaration_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_declaration_list(GLSLParser.Struct_declaration_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#struct_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_declaration(GLSLParser.Struct_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#struct_declarator_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_declarator_list(GLSLParser.Struct_declarator_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#struct_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_declarator(GLSLParser.Struct_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializer(GLSLParser.InitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#initializer_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializer_list(GLSLParser.Initializer_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#declaration_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration_statement(GLSLParser.Declaration_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(GLSLParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#simple_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_statement(GLSLParser.Simple_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#compound_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompound_statement(GLSLParser.Compound_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#statement_no_new_scope}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement_no_new_scope(GLSLParser.Statement_no_new_scopeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#compound_statement_no_new_scope}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompound_statement_no_new_scope(GLSLParser.Compound_statement_no_new_scopeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#statement_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement_list(GLSLParser.Statement_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#expression_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_statement(GLSLParser.Expression_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#selection_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelection_statement(GLSLParser.Selection_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#selection_rest_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelection_rest_statement(GLSLParser.Selection_rest_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondition(GLSLParser.ConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#switch_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_statement(GLSLParser.Switch_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#case_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_label(GLSLParser.Case_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#iteration_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIteration_statement(GLSLParser.Iteration_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#for_init_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_init_statement(GLSLParser.For_init_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#for_rest_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_rest_statement(GLSLParser.For_rest_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#jump_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJump_statement(GLSLParser.Jump_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#external_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternal_declaration(GLSLParser.External_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link GLSLParser#function_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_definition(GLSLParser.Function_definitionContext ctx);
}