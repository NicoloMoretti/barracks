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
