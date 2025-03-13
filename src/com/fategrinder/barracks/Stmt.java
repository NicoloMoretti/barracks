package com.fategrinder.barracks;

import java.util.List;
import java.util.Map;
import com.fategrinder.barracks.Expr.Literal;

abstract class Stmt {

  interface Visitor<R> {
    R visitBlockStmt(Block stmt);
    R visitExpressionStmt(Expression stmt);
    R visitIfStmt(If stmt);
    R visitWhileStmt(While stmt);
    R visitLogicalGroupingStmt(LogicalGrouping stmt);
    R visitPrintStmt(Print stmt);
    R visitVarStmt(Var stmt);
    R visitConstStmt(Const stmt);
    R visitBreakStmt(Break stmt);
    R visitContinueStmt(Continue stmt);
    R visitDisableSelfStmt(DisableSelf stmt);
    R visitLoadIfStmt(LoadIf stmt);
    R visitParamInitializationStmt(ParamInitialization stmt);
    R visitParamAssignmentStmt(ParamAssignment stmt);
    R visitFunctionDefinitionStmt(FunctionDefinition stmt);
    R visitReturnStmt(Return stmt);
  }
  abstract <R> R accept(Visitor<R> visitor);

  static class Block extends Stmt {
    Block(List<Stmt> statements) {
      this.statements = statements;
    }

    final List<Stmt> statements;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }
  }

  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    final Expr expression;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
  }

  static class If extends Stmt {
    If(List<Stmt> condition, List<Stmt> thenBranch, Stmt.If elifBranch, List<Stmt> elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elifBranch = elifBranch;
      this.elseBranch = elseBranch;
    }

    final List<Stmt> condition;
    final List<Stmt> thenBranch;
    final Stmt.If elifBranch;
    final List<Stmt> elseBranch;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }
  }

  static class While extends Stmt {
    While(List<Stmt> condition, List<Stmt> body) {
      this.condition = condition;
      this.body = body;
    }

    final List<Stmt> condition;
    final List<Stmt> body;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }
  }

  static class LogicalGrouping extends Stmt {
    LogicalGrouping(Token operator, List<Stmt> operands) {
      this.operator = operator;
      this.operands = operands;
    }

    final Token operator;
    final List<Stmt> operands;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalGroupingStmt(this);
    }
  }

  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    final Expr expression;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }
  }

  static class Var extends Stmt {
    Var(Token name, Expr initializer, TokenType type) {
      this.name = name;
      this.initializer = initializer;
      this.type = type;
    }

    final Token name;
    final Expr initializer;
    final TokenType type;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }
  }

  static class Const extends Stmt {
    Const(Token name, Literal value) {
      this.name = name;
      this.value = value;
    }

    final Token name;
    final Literal value;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitConstStmt(this);
    }
  }

  static class Break extends Stmt {
    Break(Token name) {
      this.name = name;
    }

    final Token name;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }
  }

  static class Continue extends Stmt {
    Continue(Token name) {
      this.name = name;
    }

    final Token name;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitContinueStmt(this);
    }
  }

  static class DisableSelf extends Stmt {
    DisableSelf(Token name) {
      this.name = name;
    }

    final Token name;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitDisableSelfStmt(this);
    }
  }

  static class LoadIf extends Stmt {
    LoadIf(boolean ifDefined, List<Stmt> statements, List<Stmt> elseStatements, Token symbol) {
      this.ifDefined = ifDefined;
      this.statements = statements;
      this.elseStatements = elseStatements;
      this.symbol = symbol;
    }

    final boolean ifDefined;
    final List<Stmt> statements;
    final List<Stmt> elseStatements;
    final Token symbol;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLoadIfStmt(this);
    }
  }

  static class ParamInitialization extends Stmt {
    ParamInitialization(Token alias, Token actualParameterName) {
      this.alias = alias;
      this.actualParameterName = actualParameterName;
    }

    final Token alias;
    final Token actualParameterName;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitParamInitializationStmt(this);
    }
  }

  static class ParamAssignment extends Stmt {
    ParamAssignment(Token alias, Token actualParameterName) {
      this.alias = alias;
      this.actualParameterName = actualParameterName;
    }

    final Token alias;
    final Token actualParameterName;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitParamAssignmentStmt(this);
    }
  }

  static class FunctionDefinition extends Stmt {
    FunctionDefinition(Token name, Token returnType, Map<String,TokenType> arguments, Block body) {
      this.name = name;
      this.returnType = returnType;
      this.arguments = arguments;
      this.body = body;
    }

    final Token name;
    final Token returnType;
    final Map<String,TokenType> arguments;
    final Block body;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionDefinitionStmt(this);
    }
  }

  static class Return extends Stmt {
    Return(Token name, Expr expression) {
      this.name = name;
      this.expression = expression;
    }

    final Token name;
    final Expr expression;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }
  }

}
