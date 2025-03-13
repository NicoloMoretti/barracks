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
