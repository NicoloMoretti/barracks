# Scripting guide

## Basics


> [!WARNING]  
> I will assume basic knowledge of '.per' scripting.

> [!IMPORTANT]  
> NEWLINE characters / '\n' are meaningful in barracks. They are used to TERMINATE statements. Adding more than one new line between statements, however, is allowed and has no effect.

> [!TIP]  
> Whenever you think you are seeing real 'Barracks' code in the documentation, you can easily copy it and try it yourself, if you have Barracks set up.

> [!TIP]  
> Rules split automatically if too long.

# Getting started

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
```text

(defrule
  (true)
=>
  (up-chat-data-to-self "My number: %d." c: 5)
)
```


.brk:

```text
(if
  (true)
=>
  (up-chat-data-to-self "My number: %d." 5)
)

```

Notice: we dropped the 'c:', numbers are resolved automatically to their integer value

Now, if we wante to print the result of an arithmetic expressions we can do:

```

(if
  (true)
=>
  (up-chat-data-to-self "My number: %d." 5+3*2)
)

```
Which will print "My number: 11."


Mathematic expressions can be of any length.

Supported operators are +,-,*,/,%, and '()' parenthesis for grouping for precedence.

Standard operators precedence is applied.


Notice: the division operator '/' defaults to truncated integer division. Example: 8/7 results in 0.

It previously used to be "z/" in .per, but since rounding division was bugged and unexpected compared to any other programming language, I decided to default to truncated division.

If you still wish to round you can use the additional operator '\~/' which applies the bugged default .per division. Example: 8~/7 results in 1.


## Saving and reading values from memory

The goals array has been removed!

If we want to save the result of a calculation we'll need to use a variable:

```text
int myVariable

(if
  (true)
=>
  myVariable := -12 +3
  (up-chat-data-to-self "My number: %d." myVariable)
)

; This code will print "My number: -9."
; this is a comment, .brk still uses ';' for comments
```

We created a new variable of name "myVariable" and of type "int".

A variable is like a named box to store values in and to read them at a later time, like a goal on the goals array.

The type "int" means that the variable can only contain a value of type integer.

When we use the assignment operator ':=':
1. First the right hand side will be resolved to a number, so "-12 +3" will result in -9
2. Then the result is saved into the left hand side, in this case "myVariable"

Notice that the variable resolves automatically to the number it contains when used inside an expression, so we will print "My number: -9.".

If we want to increase our variable by +1, we can simply do:

```text
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

```

First the right side of the ':=' gets resolved, then it gets assigned to the left side.

```text

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

```

Another type of variable is "point"

```text

point myFirstPoint

(if
  (true)
=>
  myFirstPoint.x := 10
  myFirstPoint.y := 20
  (up-chat-data-to-self "X coordinate: %d." myFirstPoint.x) ;prints 10
  (up-chat-data-to-self "Y coordinate: %d." myFirstPoint.y) ;prints 20
)

```text

Notice we can assign or read the x/y fields of a point variable by using the '.x' or '.y' after it.

There's also a syntax to talk about points directly:

```text

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

```

There's also point math:

```text

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

```

Points can be summed/subtracted together or multiplied by a scalar (integer)


There's also the 'timer' variable type, for example if we need 3 timers we can do:

```text

timer t-oven
timer t-microwave
timer t-alarm

```

They can be used only inside commands that ask for timers, and commands that ask for timers can only accept them.


Notice that until now we wrote (true) int the condition, but we could have also wrote 'true' without '()', as true/false are considered primitive values, just like the number (3) resolves to 3.

Barracks introduces 'else' blocks

```text

(if
  ;condition
=>
  ;do this if condition is true
)
(else
  ;do this only if condition was false
)

```

If the condition is true, then the body of the 'if' will be be executed, *else* the body of the else will be executed.

If the conditions was true, the 'else' 's body would be skipped.

```text

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

```

If you want to write a an "if true" rule you can also do it more coincesly by writing an "unconditional block":

```text

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

```

All, control structures, including 'if' 'else' and 'blocks' can be nested into each other in any combination:

```text

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

```

You can put as much nesting as you want, but it will still cost as many defrules as it would to not do it. Use it only when it makes your code better.

## elif

There is one common structure that might occur when nesting 'if' and 'else' blocks:

(this might look confusing)

```text

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

```

The idea is that you want to do only one of multiple mutually exclusive actions, and each action has a condition associated, and you only want to execute the first one that finds it's condition to be true.

Since this gets ugly quickly and hard to understand, you can instead use 'elif' which stands for "else if":

```text

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

```

'elif' stands for 'else if'. Only the first condition to be true will execute it's own body.

## while loops

A while loop allows to loop over a section of code based on a looping condition.

```text

(while
  ; condition
=>
  ; do work
  ; do more work
  ;jump back to the conditions
)

```

When the while block is first encountered, the condition is checked. If the conditions results true, then the body is exectued. After the body is executed, the code jumps back to the condition, and the process is repeated.

Whenever the conditions results false, the body is skipped and the code resumes executions to after the while block.

```text

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

```

Will print 0,1,2,3,4

Be careful of infinite loops! If the condition is never false, Aoe2 will crash.

If needed, loops can be nested inside each other.

```text

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

```

Check if it works as you expected.

An additional 2 keywords are available for while loops: 'break' and 'continue'

If 'break' is encountered, it will jump outside of the innermost loop, break out of it:

```text

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

```

This will terminate the loop early.

'continue' ends the *current* iteration of the innermost loop, jumping immedietly to the condition

```text

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

```

This will skip the printing of "3"


Variables have a 'scope'.

For now we only say variables declared outside of blocks, in the 'global' scope.

But we could have also declare variables inside a block:

```text

int gloabalVariable

(if
  true
=>
  int localVariable
  (do-nothing)
)

```

A variable created inside a block is called 'local' and exists only inside that block, and it's children.

```text

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

```

Global variables are the only ones that can save data between multiple scripts passes throghout the whole game.

Local variables exists in the block they are declared in and its children blocks.


You cannot declare two variables with the same name in exactly the same scope.

However since local variables don't exist anymore outside their scope, you can reuse their names


The programming advice is that variables should exist for as short as possible and close to where they are used, and made global only if they are used to store data through multiple script passes or all around the code.

In this way you will not be greeted with a (too) huge wall of text of variables declrations when you open a .brk file, and it will be easier to track what a variable is for.


Shadowing is also supported:

```text

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

```

Basically, in an inner scope you can declare a variable with the same name as one in the outside scope, and it will temporarly 'shadow' the more global one, until it's destroyed.


Inner variables can be declared and initialized in one line:


```text

(if
  true
=>
  int myVariable := 17
  (up-chat-data-to-self "This has indeed been initialized : %d." myVariable) ; prints 17
)

```

## Comments

Single line comments are supported with ';' Multiline comments are also supported:

```text

; this is a comment

/*
  All this block
        is
    Commented out
*/

```


## Conditions


Conditions now allow for easy comparisons with operators <,>,<=,=>,==,!=

The left hand side and right hand side resolve types automatically and also resolve expressions if necessary


```text

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

```

Conditions allow to assign to variables like .per did, and a line that assigns to a variable resolves to a 'true' value

```text

int myVar
(if
  myVar := 100
=>
  (chat-to-all "True!") ; prints True!
)

```

Just like in .per, multiple conditions are automatically put in an AND

```text

(if
  ; condition 1
  ; condition 2
  ; condition 3
=>
  (chat-to-all "True!") ; prints True!
)

```

condition 1, 2 and 3 are all in AND, if one is false, the body will be skipped.

Not only that, but it might be lesser know that in .per conditions in there 'shorcircuit', meaning that if condition 2 was false, condition 3 would outright be skipped.

This property has been mantained in barracks.


### Logical operators


.per required to pair logical operators in this way :

```text

(or
  cond 1
  (
    cond 2
    cond 3
  )
)

```

This is because and/or accepted maximum two arguments, barracks allows for more


For example, this.per:

```text

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

```

In barracks looks like this:

```text

(or
  cond 1
  cond 2
  cond 3
  cond 4
)

```

Only the operators and/or/not are supported.

As of now a 'not' would look like this


```text

(not
  cond 1
)

```

In .per conditions inside logical groupings do NOT shortcircuit, meaning that in an AND for example, if one condition was false, the others would still be checked. This behavior has been kept for .brk


## Strategic Numbers

SNs can be easly read and modified by doing

```text

(
  sn-example-sn-name := 30
  sn-example-sn-name := sn-example-sn-name := 30 +1
)

```

## Commands

Commands still work mostly the same as .per.

1. First change:

  They drop all the g: c: s:
  
2. Second change:

  If the command used to change goals, now it doesn't, and instead resolves to a value
  For exemple, (up-lerp-tiles <Point1> <Point2> <Value>) will not modify Point1.
  
  If you whish to use the new calculated point, you can do:
  
  aVariablename := (up-lerp-tiles <Point1> <Point2> <Value>) + <2,19>

  For example here we save the calculated point after adding an offset to it, and we can still use both point1 and point2 for further computations without needing to save them in temporary goals.

  Since commands resolves to values, they can be nested inside each other to act as parameters of other commands if desired.


  Example:

```text

(
  (up-chat-data-to-self "This is my military population : %d !" (up-get-fact military-population) )
)

```

  Of course it's up to you if and when you wanna nest them.

  > [!TIP]  
  > The parameter '0' for FactIDs that do not need any FactParameter is no longer needed. Just mentioning it if you noticed something weird.

## Functions

In barracks it's possible to define custom "functions".

There are 3 native functions already implemented by default: min(), max(), abs().

Functions are similar to commands, but the syntax is slightly different.

The name of the function comes right before the paranthesis that will contain the arguments.


Example:

```text

(
  (up-chat-data-to-self "The biggest number is : %d !" max(12, 60) )
)

```

Notice that parameters inside function are separated by a comma ',' too!

Let's define a new custom function, which is a bit like a custom command made out of other basic commands:

```text

func void updateGathererPercentages(int food, int wood, int gold, int stone) (
  sn-food-gatherer-percentage := food
  sn-wood-gatherer-percentage := wood
  sn-gold-gatherer-percentage := gold
  sn-stone-gatherer-percentage := stone
)

```

If we wanted to use our new function we could do:

```text

(
  updateGathererPercentages(50, 30, 10, 10)
)

```

> [!NOTE]  
> Functions are executed **only** when called, they are not part of the regular sequential stream of 'rules'.

> [!TIP]  
> Functions can be defined both before or after their first use in the file, and can interact with global variables defined both before or after their own definition.



This function call will execute the code inside the block of it's own definition.

Let's take a closer look to the definition syntax:


The keyword "func" denotes the start of a function definition, the second words is the return type, if it had been 'int' it would have meant that the function would have returned/resolved to a value, 'void' means that it doesn't return a number or anything really.

Then, after the return type, we must specify the name of the new function, followed by '(', some *facultative* parameters, and a closing ')'. then we open a new '(' and define the actual body of the function, what it has to do when called, and when we are done we close it with a ')'.


We can define a function that takes any amount of parameters, 0, 1, 2, etc..., we must specify a type for each parameter and name to reference it when we write the body of the function.


The parameters will be properly defined local variables inside the function, which will shadow eventual variables of the same name in the scope the function has been called in, meaning we don't have to worry about conflicting naming.


> [!WARNING]  
> For reasons I digress from here, no spaces ' ' are allowed between the function name and the opening paranthesis for it's own parameters. So max (12,60) is illegal, and max(12, 60) is fine. Spaces anywhere else (in reasonable places) don't matter.

If we want to define a function that returns a number, we must use the 'return' keyword whenever we want the function to end and return a value.

```text

func int calculatesSomethingCrazy(int a, int b, int c) (
  return a * b * c - 100 * a
)

(
  int numberINeeded = calculatesSomethingCrazy(10,20,30)
)

```

Notice that in the function definition we specified 'int' instead of 'void'

> [!TIP]  
> The 'return' keyword is a bit like the equivalent of 'break' in loops for function. Encountering a "return" can end a function early. The return keyword is not necessary for 'void' functions, but can be used without arguments to break out of the function earlier. This is often used to simplify code.

Example of using 'return' to exit function:

```text

func void amIHungry(int hunger) (
  (if
    hunger > 30
  =>
    (chat-to-all "I'm not hungry!")
    return
  )
  (chat-to-all "I'm hungry!")
)

(
  amIHungry(29)
)

```

Functions can call other functions inside of them, or even themeselves!

```text

func int factorial(int n) (
    (if 
         n <= 1
    =>
         return 1
    )
    (else
         return n * factorial(n - 1)
    )
    return 0
)

```


> [!WARNING]  
> As of now only native functions such as max/min/abs can be used inside conditions. This is an unfortunate current limitation of barracks.


## disable-self


disable-self is still here and has been upgraded to disable it's block and all of it's children block!

It's been promoted to a keyword like 'return', 'break', 'continue', so it dropped it's parenthesis.

More importantly you are now required to put it at the start of the block it has to disable:

```text

(
  disable-self
  ; stuff
  ; stuff
)

```

In this way, when you have long blocks with many child blocks nested inside, it's immedietly clear that the everything is subject to 'disable-self', if you don't see it immedietly, it's not there.

## Constants

pre-processor constants can be defined in the global scope by doing

```text

const PI 314

```

## load-if


load-ifs still work the same way as .per.

One important note is that sometimes it's necessary to declare parameters for later, for example, in .per we would do:

```text

#load-if-defined AZTEC-CIV	
    (defconst scout-type -267)	;Eagle warrior line
#else	
#load-if-defined INCAN-CIV	
    (defconst scout-type -267)	
#else	
#load-if-defined MAYAN-CIV	
    (defconst scout-type -267)	
#else	
    (defconst scout-type -286)	;Scout cavalry line			
#end-if	
#end-if	
#end-if

```

But barracks would refuse to let you use 'scout-type' inside the commands you want to, since it would tell you the constant is a number and not a parameter containing the unit line.


To avoid that you have to do define a parameter variable:


```text

param scout-type := scout-cavalry-line

; the "scout-type" variable will now accecept only other "unit lines" inside of it, and will be available to be used in all commands that accept such type of parameter

#load-if-defined AZTEC-CIV	
    scout-type :=  eagle-warrior-line
#else	
#load-if-defined INCAN-CIV	
    scout-type :=  eagle-warrior-line
#else	
#load-if-defined MAYAN-CIV	
    scout-type :=  eagle-warrior-line	
#end-if	
#end-if	
#end-if

```

'param' varaibles must be initialized when defined to make clear what parameter class they will contain.
For now parameters can only be declared and changed in the global scope, they are actaully compiled down to defconsts in .per... a quick patchwork.


Inside load-ifs it's illegal to define new variables, since barracks cannot be sure about what will actually happen at compile time.
load-ifs can contain any kind of blocks (if/else/elif/while/blocks), they are just like .per.

## Default constants


All default constants (parameters definition...) are atuomatically imported.

There were some missing ones that I had to invent names for (since I couldn't find a convention, and i had to keep the commands type-safe):

ActionId  -1 -> actionid-any
OrderId -1 -> orderid-any
AttackStance -1 -> stance-unchanged
Formation -1 -> formation-unchanged

## Removed commands

Some commands have been permanently removed in barracks because of contrasting philosophy with the language.

Removed commands:

goal
set-goal
up-modify-goal
up-compare-goal

set-strategic-number
strategic-number
up-modify-sn
up-compare-sn

up-modify-flag
up-compare-flag

up-add-point
up-copy-point

up-set-indirect-goal
up-get-indirect-goal

up-get-rule-id
up-jump-direct
up-jump-dynamic
up-jump-rule

xs-script-call (might return in I don't find a better way to use it)

up-get-search-state

## Commands not currently planned to be added
up-get-threat-data
up-get-victory-data
set-shared-goal,  shared-goal (to be replaced with some convention)

## AIs communicating together

Commands related to AIs communicating together are currenly disabled

up-get-shared-goal
up-set-shared-goal
up-allied-goal
up-allied-sn
allied-goal and allied-sn in FactIds

I'm not aware of AI's communicating together right now, but I might be living under a rock.

Regardless, I hope that some nice convention might be found, so that whenever a new "multi-agent" barracks AI is made, it can communicate with older ones without the need to check their own code, just by abiding by the same convention...

## Search states

Search states don't need to be declared anymore, and they are pre-declared read-only variables in barracks.
In an attempt to standardize things used often, they have been called:

search-local-total, search-local-last, search-remote-total, search-remote-last

You will always find them updated after a serach (but not after manually modifying lists yourself), removing the command '(up-get-search-state)'.
If you wish to save a previous search state value before a new serach, copy the value of the search state you want into a support variable.
