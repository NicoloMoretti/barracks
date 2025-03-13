# Scripting guide

## Basics


> [!WARNING]  
> I will assume basic knowledge of '.per' scripting.

> [!IMPORTANT]  
> NEWLINE characters / '\n' are meaningful in barracks

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



