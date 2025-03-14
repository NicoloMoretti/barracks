# Barracks

## What it's about

Barracks is an alternative programming language to script bots/AIs for Age Of Empires 2 DE.
It aims to provide an additional layer of abstraction compared to regular '.per' scripting.
The focus is to make it easier to read/write code compared to writing '.per' files directly, while staying familiar and intuitive.
Code written in Barracks is automatically compiled down to .per files through the Barracks compiler.

## How to install it


There isn't a graphic interface, you are supposed to interact with it through the terminal, by executing it with a '.brk' file path as an argument.

A '.per' file (and a '.ai' file) will be generated in the same location as the '.brk' file, or eventual errors will be reported within the terminal.

### Automatic installation instructions:

Will setup barracks to work from anywhere within the terminal.

#### Automatic installation for **Windows**:

1. Downalod "Barracks_installation_Windows.zip".
2. Extract the ZIP file. You should see three files inside the extracted folder.
3. Right-click install.bat and select "Run as administrator".

If it worked, you are done.

You can try in the cmd:
```text
barracks --version
```
If you see the version of Barracks displayed, it's set up correctly!

#### Automatic installation for **Linux**:

1. Downalod "Barracks_installation_Linux.zip".
2. Extract the ZIP file. You should see three files inside the extracted folder.
3. Open a terminal in the extracted folder:
  - Right-click an empty space in the folder.
  - Select "Open in Terminal" from the dropdown menu.
4. Run the following commands in the terminal:

```text
chmod +x install.sh
sudo ./install.sh
```

If it worked, you are done.

You can try in the terminal:
```text
barracks --version
```
If you see the version of Barracks displayed, it's set up correctly!


#### Manual installation instructions:

The compiler is essentially a Java application, which can be downloaded directly (or compiled from source if preferred).

The automatic installation is just to automatically place the .jar file somewhere under PATH, with an additional script to make calling barracks from the console cleaner than doing "java -jar barracks.jar".

You are free to set up things in a different way.


## Getting started

Once you create a file with a .brk file extension, and you have the compiler installed, you just need to open a terminal window and launch Barracks with your .brk file path as argument.

To learn how to code in Barracks you can consult the [documentation](docs/scripting-guide.md) available under /docs/.
