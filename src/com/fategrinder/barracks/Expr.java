package com.fategrinder.barracks;

import java.util.List;
import java.util.Map;
import com.fategrinder.barracks.Expr.Literal;

abstract class Expr {

  interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitCallExpr(Call expr);
    R visitGetExpr(Get expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitCommandExpressionExpr(CommandExpression expr);
    R visitfunctionCallExpressionExpr(functionCallExpression expr);
    R visitSetExpr(Set expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
  }
  abstract <R> R accept(Visitor<R> visitor);

  static class Assign extends Expr {
    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    final Token name;
    final Expr value;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }
  }

  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    final Expr left;
    final Token operator;
    final Expr right;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }
  }

  static class Call extends Expr {
    Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    final Expr callee;
    final Token paren;
    final List<Expr> arguments;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }
  }

  static class Get extends Expr {
    Get(Expr object, Token name) {
      this.object = object;
      this.name = name;
    }

    final Expr object;
    final Token name;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGetExpr(this);
    }
  }

  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    final Expr expression;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }
  }

  static class Literal extends Expr {
    Literal(Object value, TokenType type) {
      this.value = value;
      this.type = type;
    }

    final Object value;
    final TokenType type;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }
  }

  static class CommandExpression extends Expr {
    CommandExpression(Token commandName, List<Expr> expressions, CommandPer commandPer) {
      this.commandName = commandName;
      this.expressions = expressions;
      this.commandPer = commandPer;
    }

    final Token commandName;
    final List<Expr> expressions;
    final CommandPer commandPer;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCommandExpressionExpr(this);
    }
  }

  static class functionCallExpression extends Expr {
    functionCallExpression(Token functionName, List<Expr> arguments, Token closingParenthesis) {
      this.functionName = functionName;
      this.arguments = arguments;
      this.closingParenthesis = closingParenthesis;
    }

    final Token functionName;
    final List<Expr> arguments;
    final Token closingParenthesis;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitfunctionCallExpressionExpr(this);
    }
  }

  static class Set extends Expr {
    Set(Expr object, Token name, Expr value) {
      this.object = object;
      this.name = name;
      this.value = value;
    }

    final Expr object;
    final Token name;
    final Expr value;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSetExpr(this);
    }
  }

  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    final Token operator;
    final Expr right;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }
  }

  static class Variable extends Expr {
    Variable(Token name) {
      this.name = name;
    }

    final Token name;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }
  }

}
