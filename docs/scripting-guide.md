# Scripting guide

## Basics


> [!WARNING]  
> I will assume basic knowledge of '.per' scripting.

> [!IMPORTANT]  
> NEWLINE characters / '\n' are meaningful in barracks. They are used to TERMINATE statements, adding more before beginning a new statements however is allowed and has no effect.

## Getting started

Just like a .per file, a .brk (barracks file extension) is read top to bottom every 1/3 of a second, a .per file is mostly made of sequential defrules.

```text

(defrule
  condition
=>
  body/actions
)

.
.
.
v


(defrule
  condition
=>
  body/actions
)


.
.
.
v

(defrule
  condition 
=>
  body/actions
)



.
.
.
v


....

```

In Barracks regular "defrules" have been replaced by "if" blocks.

```text

They look like this:

(if
  condition
=>
  body/actions
)

.
.
.
v


(if              <-     Notice: the "defrule" keyword has been replaced by "if"
  condition
=>
  body/actions
)


.
.
.
v

(if
  condition
=>
  body/actions
)


.
.
.
v

....

```

You can safely assume it will work the same: if the condition of an 'if' statement is true, the body will be executed

## Basic Mathematics

Let's chat a number in .per and in .brk:

.per:

(defrule
  (true)
=>
  (up-chat-data-to-self "My number: %d." c: 5)
)

.brk:

(if
  (true)
=>
  (up-chat-data-to-self "My number: %d." 5)
)

Notice: we dropped the 'c:', numbers are resolved automatically to their integer value

Now, if we wante to print the result of an arithmetic expressions we can do:

(if
  (true)
=>
  (up-chat-data-to-self "My number: %d." 5+3*2)
)

Which will print "My number: 11."

Mathematic expressions can be of any length.
Supported operators are +,-,*,/,%, and '()' parenthesis for grouping for precedence.
Standard operators precedence is applied.

Notice: the division operator '/' defaults to truncated integer division. Example: 8/7 results in 0.
It previously used to be "z/" in .per, but since rounding division was bugged and unexpected compared to any other programming language, I decided to default to truncated division.
If you still wish to round you can use the additional operator '~/' which applies the bugged default .per division. Example: 8~/7 results in 1.

## Saving and reading values in memory

The goals array has been removed!
If we want to save the result of a calculation we'll need to use a variable:


int myVariable

(if
  (true)
=>
  myVariable := -12 +3
  (up-chat-data-to-self "My number: %d." myVariable)
)

; This code will print "My number: -9."
; this is a comment, .brk still uses ';' for comments

We created a new variable of name "myVariable" and of type "int".
A variable is like a named box to store values in and to read them at a later time, like a goal on the goals array.
The type "int" means that the variable can only contain a value of type integer.
when we use the assignment operator ':=':
1. First the right hand side will be resolved to a number, so "-12 +3" will result in -9
2. Then the result is saved into the left hand side, in this case "myVariable"

Notice that the variable resolves automatically to the number it contains when used inside an expression, so we will print "My number: -9.".

If we want to increase our variable by +1, we can simply do:

int myVariable

(if
  (true)
=>
  myVariable := -12 +3
  (up-chat-data-to-self "My number: %d." myVariable) ;prints -9
  myVariable := myVariable +1
  (up-chat-data-to-self "My number: %d." myVariable) ;prints -8
  (up-chat-data-to-self "My number: %d." myVariable+10) ;prints +2
  (up-chat-data-to-self "My number: %d." myVariable) ;prints -8
)

First the right side of the ':=' gets resolved, then it gets assigned to the left side.

int myVariable
int myVariable2 ;notice we declared another one

(if
  (true)
=>
  myVariable := 10
  myVariable2 := myVariable
  myVariable := 100
  (up-chat-data-to-self "My number: %d." myVariable) ;prints 100
  (up-chat-data-to-self "My number: %d." myVariable2) ;prints 10
)


Another type of variable is "point"


point myFirstPoint

(if
  (true)
=>
  myFirstPoint.x := 10
  myFirstPoint.y := 20
  (up-chat-data-to-self "X coordinate: %d." myFirstPoint.x) ;prints 10
  (up-chat-data-to-self "Y coordinate: %d." myFirstPoint.y) ;prints 20
)

Notice we can assign or read the x/y fields of a point variable by using the '.x' or '.y' after it.

There's also a syntax to talk about points directly:

point myFirstPoint

(if
  (true)
=>
  myFirstPoint := <10,20>      ; notice the <x value, y value> syntax
  (up-chat-data-to-self "X coordinate: %d." myFirstPoint.x) ;prints 10
  (up-chat-data-to-self "Y coordinate: %d." myFirstPoint.y) ;prints 20

  (up-set-precise-target-point <10,20>)  ; valid
  (up-set-precise-target-point myFirstPoint)  ; also valid
)

There's also point math:

point myFirstPoint

(if
  (true)
=>
  myFirstPoint := <10,20> + <2,3>  ; myFirstPoint variable now contains the value <12,23>
  (up-chat-data-to-self "X coordinate: %d." myFirstPoint.x) ;prints 12
  (up-chat-data-to-self "Y coordinate: %d." myFirstPoint.y) ;prints 23
  
  myFirstPoint := -myFirstPoint
  (up-chat-data-to-self "X coordinate: %d." myFirstPoint.x) ;prints -12
  (up-chat-data-to-self "Y coordinate: %d." myFirstPoint.y) ;prints -23

  myFirstPoint := myFirstPoint + <2,3>
  (up-chat-data-to-self "X coordinate: %d." myFirstPoint.x) ;prints -10
  (up-chat-data-to-self "Y coordinate: %d." myFirstPoint.y) ;prints -20

  myFirstPoint := myFirstPoint *100
  (up-chat-data-to-self "X coordinate: %d." myFirstPoint.x) ;prints -1000
  (up-chat-data-to-self "Y coordinate: %d." myFirstPoint.y) ;prints -2000

  myFirstPoint := myFirstPoint /100
  (up-chat-data-to-self "X coordinate: %d." myFirstPoint.x) ;prints -10
  (up-chat-data-to-self "Y coordinate: %d." myFirstPoint.y) ;prints -20
)

Points can be summed/subtracted together or multiplied by a scalar (integer)


Notice that until now we wrote (true) int the condition, but we could have also wrote 'true' without '()', as true/false are considered primitive values, just like the number (3) resolves to 3.

Barracks introduces 'else' blocks

(if
  ;condition
=>
  ;do this if condition is true
)
(else
  ;do this only if condition was false
)

If the condition is true, then the body of the 'if' will be be executed, *else* the body of the else will be executed.
If the conditions was true, the 'else' 's body would be skipped.


(if
  true
=>
  (chat-to-all "the condition was true")
)
(else
  (chat-to-all "the condition was false")
)

; prints "the condition was true"

(if
  false
=>
  (chat-to-all "the condition was true")
)
(else
  (chat-to-all "the condition was false")
)

; prints "the condition was false"


If you want to write a an "if true" rule you can also do it more coincesly by writing an "unconditional block":

(if
  true
=>
  (chat-to-all "I always do this")
  ; more stuff..
  ; more code...
)

.
.
.
V

(
  (chat-to-all "I always do this")
  ; more stuff..
  ; more code...
)


All, control structures, including 'if' 'else' and 'blocks' can be nested into each other in any combination:

(if
  ; condition 1
=>
  ; some code that depends on condition1
  (if
    ; condition 2
  =>
    ; some code that depends on condition 1 AND condition 2
  )
  ; some code that depends on condition 1
  (if
    ; condition3
  =>
    ; some code that depends on condition 1 AND condition 3
  )
)

You can put as much nesting as you want, but it will still cost as many defrules as it would to not do it. Use it only when it makes your code better.

## elif

There is one common structure that might occur when nesting 'if' and 'else' blocks:
(this might righly look confusing)

(if
  ; condition 1
=>
  ; do this if cond 1
)
(else
  (if
    ; condition 2
  =>
    ; do this if cond 2 and not cond 1
  )
  (else
    (if
      ; condition 3
    =>
      ; do this if cond 3 and not cond 1 and not cond 2
    )
  )
)

The idea is that you want to do only one of multiple mutually exclusive actions, and each action has a condition associated, and you only want to execute the first one that finds it's condition to be true.

Since this gets ugly quickly and hard to understand, you can instead use 'elif' which stands for "else if":

(if
  ; condition 1
=>
  ; do this if cond 1
)
(elif
  ; condition 2
=>
  ; do this if cond 2 and not cond 1
)
(elif
  ; condition 3
=>
  ; do this if cond 3 and not cond 1 and not cond 2
)

'elif' stands for 'else if'. Only the first condition to be true will execute it's own body.

## while loops

A while loop allows to loop over a section of code based on a looping condition.


(while
  ; condition
=>
  ; do work
  ; do more work
  ;jump back to the conditions
)

When the while block is first encountered, the condition is checked. If the conditions results true, then the body is exectued. After the body is executed, the code jumps back to the condition, and the process is repeated.
Whenever the conditions results false, the body is skipped and the code resumes executions to after the while block.

int i

(
  i := 0
)

(while
  i < 5
=>
  (up-chat-data-to-self "'i' is: %d." i)
  i := i + 1
)

Will print 0,1,2,3,4

Be careful of infinite loops! If the condition is never false, Aoe2 will crash.

If needed, loops can be nested inside each other.

int x
int y

(
    x := 0
)

(while
    x < 5
=>
    (up-chat-data-to-self "'x' is: %d." x)
    y := 0
    (while
        y < 5
    =>
        (up-chat-data-to-self "'y' is: %d." y)
        y := y + 1
    )
    x := x + 1
)

Check if it works as you expected.

An additional 2 keywords are available for while loops: 'break' and 'continue'

If 'break' is encountered, it will jump outside of the innermost loop, break out of it:

int i

(
  i := 0
)

(while
  i < 5
=>
  (if
    i == 3
  =>
    break
  )
  (up-chat-data-to-self "'i' is: %d." i)
  i := i + 1
)

This will terminate the loop early.

'continue' ends the *current* iteration of the innermost loop, jumping immedietly to the condition

int i

(
  i := 0
)

(while
  i < 5
=>
  (if
    i == 3
  =>
    i := i + 1
    continue
  )
  (up-chat-data-to-self "'i' is: %d." i)
  i := i + 1
)

This will skip the printing of "3"


Variables have a 'scope'.
For now we only say variables declared outside of blocks, in the 'global' scope.
But we could have also declare variables inside a block:

int gloabalVariable

(if
  true
=>
  int localVariable
  (do-nothing)
)

A variable created inside a block is called 'local' and exists only inside that block, and it's children.

int gloabalVariable

(if
  true
=>
  int localVariable
  gloabalVariable := 20
  localVariable := 100
  (up-chat-data-to-self "My local variable is: %d." gloabalVariable) ; prints 20
  (up-chat-data-to-self "My local variable is: %d." localVariable) ; prints 100
  (
    (up-chat-data-to-self "My local variable is: %d." localVariable) ; prints 100, still exists here
  )
)

(
  (up-chat-data-to-self "My local variable is: %d." gloabalVariable) ; prints 20
  (up-chat-data-to-self "My local variable is: %d." localVariable) ; ERROR! The local variable doesn't exists here!
)

Global variables are the only ones that can save data between multiple scripts passes throghout the whole game.
Local variables exists in the block they are declared in and its children blocks.

You cannot declare two variables with the same name in exactly the same scope.
However since local variables don't exist anymore outside their scope, you can reuse their names

The programming advice is that variables should exist for as short as possible and close to where they are used, and made global only if they are used to store data through multiple script passes or all around the code.
In this way you will not be greeted with a (too) huge wall of text of variables declrations when you open a .brk file, and it will be easier to track what a variable is for.

Shadowing is also supported:

; How loud is the sound?
int volume := 11


; Calculate size of 3x4x5 cuboid.
(
  int volume = 3 * 4 * 5
  (up-chat-data-to-self "The cube has a volume of : %d." volume) ; prints 3 * 4 * 5
)

(
  (up-chat-data-to-self "The noise volume is : %d." volume) ; prints 11
)

Basically, in an inner scope you can declare a variable with the same name as one in the outside scope, and it will temporarly 'shadow' the more global one, until it's destroyed.

Inner variables can be declared and initialized in one line:

(if
  false
=>
  int myVariable := 17
)

## comments

Single line comments are supported with ';' Multiline comments are also supported:

; this is a comment

/*
  All this block
        is
    Commented out
*/


## conditions

Conditions now allow for easy comparisons with operators <,>,<=,=>,==,!=
The left hand side and right hand side resolve types automatically and also resolve expressions if necessary

int myVar

(
  myVar := 10
)

(if
  myVar +3 >= 12-1
=>
  (up-chat-data-to-self "The var +3 was : %d." myVar +3)
  (up-chat-data-to-self "Which was greater than : %d." 12-1)
)
(else
  (chat-to-all "No!")
)


Conditions allow to assign to variables like .per did, and a line that assigns to a variable resolves to a 'true' value

int myVar
(if
  myVar := 100
=>
  (chat-to-all "True!") ; prints True!
)

Just like in .per, multiple conditions are automatically put in an AND

(if
  ; condition 1
  ; condition 2
  ; condition 3
=>
  (chat-to-all "True!") ; prints True!
)

condition 1, 2 and 3 are all in AND, if one is false, the body will be skipped.

Not only that, but it might be lesser know that in .per conditions in there 'shorcircuit', meaning that if condition 2 was false, condition 3 would outright be skipped.
This property has been mantained in barracks.

### Logical operators

.per required to pair logical operators in this way :

(or
  cond 1
  (
    cond 2
    cond 3
  )
)

This is because and/or accepted maximum two arguments, barracks allows for more

For example, this.per:

(or
  cond 1
  (
    cond 2
    (or
      cond 3
      cond 4
    )
  )
)

In barracks looks like this:

(or
  cond 1
  cond 2
  cond 3
  cond 4
)

Only the operators and/or/not are supported.
As of now a 'not' would look like this
(not
  cond 1
)

In .per conditions inside logical groupings do NOT shortcircuit, meaning that in an AND for example, if one condition was false, the others would still be checked. This behavior has been kept for .brk
