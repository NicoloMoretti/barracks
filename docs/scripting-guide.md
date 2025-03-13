# Scripting guide

### Requierements

I will assume basic knowledge of '.per' scripting.

## Basics

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


In Barracks regular "defrules" have been replaced by "if" blocks.

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

You can safely assume it will work the same, if the condition of an 'if' statement is true, the body will be executed
