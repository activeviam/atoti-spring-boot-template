/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.conditions;

import static com.activeviam.apps.query.conditions.AndCondition.AND;
import static com.activeviam.apps.query.conditions.EqualsCondition.EQ;
import static com.activeviam.apps.query.conditions.OrCondition.OR;

import java.time.LocalDate;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.stereotype.Component;

import com.activeviam.apps.query.grammar.QueryConditionBaseVisitor;
import com.activeviam.apps.query.grammar.QueryConditionParser;

@Component
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

            if (ctx.logicalOp.getText().equalsIgnoreCase(AND)) {
                return new AndCondition(subConditions);
            } else if (ctx.logicalOp.getText().equalsIgnoreCase(OR)) {
                return new OrCondition(subConditions);
            }
        }
        // Otherwise, it is a single criteria query
        return visitChildren(ctx);
    }

    @Override
    public LogicalCondition visitPriorityQuery(QueryConditionParser.PriorityQueryContext ctx) {
        return visit(ctx.query());
    }

    @Override
    public LogicalCondition visitCriteriaQuery(QueryConditionParser.CriteriaQueryContext ctx) {
        return visit(ctx.criteria());
    }

    @Override
    public LogicalCondition visitCriteria(QueryConditionParser.CriteriaContext ctx) {
        var field = ctx.key().getText();
        var operator = ctx.op().getText();
        return parseCriteria(operator, ctx.value(), field);
    }

    @Override
    public LogicalCondition visitInput(QueryConditionParser.InputContext ctx) {
        return visit(ctx.query());
    }

    private LogicalCondition parseCriteria(String operator, QueryConditionParser.ValueContext ctx, String field) {
        if (ctx.BOOL() != null) {
            var value = Boolean.parseBoolean(ctx.getText());
            return operator.equals(EQ)
                    ? new EqualsCondition<Boolean>(field, value)
                    : new NotEqualsCondition<Boolean>(field, value);
        } else if (ctx.STRING() != null) {
            var value = ctx.getText().replace("'", "");
            return operator.equals(EQ)
                    ? new EqualsCondition<String>(field, value)
                    : new NotEqualsCondition<String>(field, value);
        } else if (ctx.NUMBER() != null) {
            var value = Double.parseDouble(ctx.getText());
            return operator.equals(EQ)
                    ? new EqualsCondition<Double>(field, value)
                    : new NotEqualsCondition<Double>(field, value);
        } else if (ctx.DATE() != null) {
            var value = LocalDate.parse(ctx.getText());
            return operator.equals(EQ)
                    ? new EqualsCondition<LocalDate>(field, value)
                    : new NotEqualsCondition<LocalDate>(field, value);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
