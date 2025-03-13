# Barracks

## What's it about

Barracks is an alternative programming language to script bots/AIs for Age Of Empires 2 DE.
It aims to provide an additional layer of abstraction compared to regular '.per' scripting.
The focus is to make it easier to read/write code compared to writing '.per', while staying familiar and intuitive.
Code written in Barracks is automatically compiled down to .per files through the Barracks compiler.

## How to install it

The compiler is essentially a Java application, which can be downloaded directly (or compiled from source if preferred).
There isn't a graphic interface, you are supposed to interact with it through the terminal, by executing it with a '.brk' file path as an argument.
A '.per' will be generated in the same location as the '.brk' file, or eventual errors will be reported within the terminal.

To make it easier to use, I suggest to download the 'barracks' shell script as well, keep it next to the Java application, and launch that instead by simply typing 'barracks' in the folder where both files are contained.
Finally, to make it possible to launch barracks from wherever in your system, indipendently of it's storage location, I would suggest to put both the 'barracks' shell script and the Java file in a directory that your PC recognizes as under 'PATH'.
