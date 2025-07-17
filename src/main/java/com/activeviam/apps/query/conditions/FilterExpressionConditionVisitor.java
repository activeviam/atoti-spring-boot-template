/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.conditions;

import java.time.LocalDate;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.activeviam.apps.query.grammar.QueryConditionBaseVisitor;
import com.activeviam.apps.query.grammar.QueryConditionParser;

public class FilterExpressionConditionVisitor extends QueryConditionBaseVisitor<LogicalCondition> {

    public static LogicalCondition parseFilterExpression(String query) {
        var lexer = new com.activeviam.apps.query.grammar.QueryConditionLexer(CharStreams.fromString(query));
        var tokens = new CommonTokenStream(lexer);
        var parser = new QueryConditionParser(tokens);
        var context = parser.input();
        var visitor = new FilterExpressionConditionVisitor();
        return visitor.visitInput(context);
    }

    @Override
    public LogicalCondition visitOperatorQuery(QueryConditionParser.OperatorQueryContext ctx) {
        // Check if the query is a composite query (with AND/OR) or a single criteria
        if (ctx.left != null && ctx.right != null) {
            var leftCond = visit(ctx.left);
            var rightCond = visit(ctx.right);
            var subConditions = List.of(leftCond, rightCond);
            if (ctx.logicalOp.getText().equalsIgnoreCase("AND")) {
                return new AndLogicalCondition(subConditions);
            } else if (ctx.logicalOp.getText().equalsIgnoreCase("OR")) {
                return new OrLogicalCondition(subConditions);
            }
        }
        // Otherwise, it is a single criteria query
        return visitChildren(ctx);
    }

    @Override
    public LogicalCondition visitNotConditionQuery(QueryConditionParser.NotConditionQueryContext ctx) {
        return new NotLogicalCondition(visit(ctx.notConditition));
    }

    @Override
    public LogicalCondition visitInConditionQuery(QueryConditionParser.InConditionQueryContext ctx) {
        var values = ctx.values;
        var field = ctx.field.getText();
        if (!values.BOOL().isEmpty()) {
            return new InLogicalCondition<Boolean>(
                    field,
                    values.BOOL().stream()
                            .map(v -> Boolean.parseBoolean(v.getText()))
                            .toList());
        } else if (!values.STRING().isEmpty()) {
            return new InLogicalCondition<String>(
                    field,
                    values.STRING().stream()
                            .map(v -> v.getText().replace("'", ""))
                            .toList());
        } else if (!values.NUMBER().isEmpty()) {
            return new InLogicalCondition<Number>(
                    field,
                    values.NUMBER().stream().map(v -> intOrDouble(v.getText())).toList());
        } else if (!values.DATE().isEmpty()) {
            return new InLogicalCondition<LocalDate>(
                    field,
                    values.DATE().stream()
                            .map(v -> LocalDate.parse(v.getText()))
                            .toList());
        } else {
            throw new UnsupportedOperationException("Measure conditions not supported in CubeRestrictions");
        }
    }

    @Override
    public LogicalCondition visitPriorityQuery(QueryConditionParser.PriorityQueryContext ctx) {
        return visit(ctx.query());
    }

    @Override
    public LogicalCondition visitLikeConditionQuery(QueryConditionParser.LikeConditionQueryContext ctx) {
        return new LikeLogicalCondition(ctx.field.getText(), ctx.criteria.getText());
    }

    @Override
    public LogicalCondition visitMeasureConditionQuery(QueryConditionParser.MeasureConditionQueryContext ctx) {
        return new MeasureCondition(
                ctx.measure.getText(),
                ctx.operator.getText(),
                Double.parseDouble(ctx.operand.getText()),
                ctx.field.getText());
    }

    @Override
    public LogicalCondition visitBetweenConditionQuery(QueryConditionParser.BetweenConditionQueryContext ctx) {
        var left = parseDateOrInt(ctx.left);
        var right = parseDateOrInt(ctx.right);
        return new BetweenLogicalCondition<Object>(ctx.field.getText(), left, right);
    }

    @Override
    public LogicalCondition visitInput(QueryConditionParser.InputContext ctx) {
        return visit(ctx.query());
    }

    private static Object parseDateOrInt(QueryConditionParser.BetweenArgContext ctx) {
        if (ctx.SEMICOL() != null) {
            return null;
        } else if (ctx.DATE() != null) {
            return LocalDate.parse(ctx.getText());
        } else {
            return intOrDouble(ctx.getText());
        }
    }

    private static Number intOrDouble(String str) {
        if (str.contains(".")) {
            return Double.parseDouble(str);
        } else {
            return Integer.parseInt(str);
        }
    }
}
