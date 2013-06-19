package com.chrisfolger.needsmoredojo.testutil;

import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.impl.JSExpressionStatementImpl;

import static org.mockito.Mockito.mock;

public class MockJSExpressionStatement extends JSExpressionStatementImpl
{
    private JSExpression expression;

    public MockJSExpressionStatement(JSExpression expression) {
        super(mock(ASTNode.class));

        this.expression = expression;
    }

    @Override
    public JSExpression getExpression()
    {
        return expression;
    }
}
