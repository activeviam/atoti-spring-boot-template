grammar QueryCondition;

// Entry point of the grammar
input
   : query EOF
   ;

// A query can be a combination of subqueries with logical operators, a parenthesized query, or a single criteria
query
   : left=query logicalOp=('AND' | 'OR') right=query #operatorQuery
//   : logicalOp=('AND' | 'OR') LPAREN argumentList? RPAREN #operatorQuery
   | LPAREN query RPAREN #priorityQuery
   | criteria #criteriaQuery
   ;

argumentList
   : (ARG_SEPARATOR query)*
   ;

// Criteria consists of a key, an operator, and a value
criteria
   : key op value
   ;

// Key is an identifier (e.g., field name)
key
   : IDENTIFIER
   ;

// Value can be a string, number, boolean, or date
value
   : STRING
   | NUMBER
   | BOOL
   | DATE
   ;

// Operators used in criteria
op
   : '=='
   | '!='
   ;

// Identifiers (e.g., field names)
ARG_SEPARATOR
    :
    ',';

NUMBER
   : ('0' .. '9')+
   ;

BOOL
    : 'true'
    | 'false'
    ;

IDENTIFIER
   : [a-zA-Z_][a-zA-Z_0-9]*
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
   : [0-9][0-9][0-9][0-9]
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

WHITESPACES
   : [ \t\r\n]+ -> skip
   ;