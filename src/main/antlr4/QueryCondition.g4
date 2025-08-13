grammar QueryCondition;

// Entry point of the grammar
input
   : query EOF
   ;

// A query can be a combination of subqueries with logical operators, a parenthesized query, or a single criteria
query
   : left=query logicalOp=('AND' | 'OR') right=query #operatorQuery
   | LPAREN query RPAREN #priorityQuery
   | field=fieldName 'IN' LSQPAREN values=valuesList RSQPAREN #inConditionQuery
   | 'NOT' notConditition=query #notConditionQuery
   | field=fieldName 'LIKE' criteria=criteriaType #likeConditionQuery
   | field=fieldName 'BETWEEN' leftParentesis=(LSQPAREN | LPAREN) left=betweenArg ',' right=betweenArg rightParentesis=(RPAREN | RSQPAREN) #betweenConditionQuery
   | measure=measureName 'AT' field=fieldName operator=('<='|'<'|'>'|'>='|'=') operand=operandType #measureConditionQuery
   ;

valuesList
   :  (NUMBER ARG_SEPARATOR?)*
      | (BOOL ARG_SEPARATOR?)*
      | (DATE ARG_SEPARATOR?)*
      | (STRING ARG_SEPARATOR?)*
   ;

betweenArg
    : SEMICOL
    | NUMBER
    | DATE
    ;

// Key is an identifier (e.g., field name)
fieldName
   : IDENTIFIER
   ;

// Key is an identifier (e.g., field name)
measureName
   : MEASURE_IDENTIFIER
   ;

criteriaType
    : STRING
    ;

operandType
    : NUMBER
    ;

//// Value can be a string, number, boolean, or date
//value
//   : STRING
//   | NUMBER
//   | BOOL
//   | DATE
//   ;

// Identifiers (e.g., field names)
// references the DIGIT helper rule

ARG_SEPARATOR
    :
    ',';

NUMBER
    :
    (PLUS_MINUS)? DIGIT+ (PT DIGIT+)?
    ;

fragment DIGIT : [0-9] ; // not a token by itself

BOOL
    : 'true'
    | 'false'
    ;

SEMICOL
    : ':'
    ;

IDENTIFIER
   : [a-zA-Z_][a-zA-Z_0-9]*
   ;

MEASURE_IDENTIFIER
   : [a-zA-Z_][a-zA-Z_0-9.]*
   ;

STRING
   : '\'' ( ('\'\'') | ('\\'+ ~'\\') | ~('\'' | '\\') )* '\''
   ;

// Date values (formatted as YYYY-MM-DD)
DATE
   : YEAR '-' MONTH '-' DAY
   ;

// Year should be four digits
YEAR
   : DIGIT DIGIT DIGIT DIGIT
   ;

// Month should be two digits (01 to 12)
MONTH
   : [0][1-9] | [1][0-2]
   ;

// Day should be two digits (01 to 31), additional logic needed for exact day validation
DAY
   : [0][1-9] | [1-2][0-9] | [3][0-1]
   ;

LPAREN
   : '('
   ;


RPAREN
   : ')'
   ;

LSQPAREN
   : '['
   ;


RSQPAREN
   : ']'
   ;

WHITESPACES
   : [ \t\r\n]+ -> skip
   ;

PT
   : '.'
   ;

PLUS_MINUS
    : '+'
    | '-'
    ;