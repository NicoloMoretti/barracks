# Barracks

## What it's about

Barracks is an alternative programming language to script bots/AIs for Age Of Empires 2 DE.
It aims to provide an additional layer of abstraction compared to regular '.per' scripting.
The focus is to make it easier to read/write code compared to writing '.per' files directly, while staying familiar and intuitive.
Code written in Barracks is automatically compiled down to .per files through the Barracks compiler.

## How to install it

### Automatic installation instructions:

Will setup barracks to work from anywhere within the terminal/cmd.

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

After installation, Barracks is available for use from the terminal/cmd.

First, create a file ending with a '.brk' extension. (If you are on Windows be careful to not get something like '.brk.txt')

Then you can open a cmd/terminal window, which is just an application that comes by default with your operative system, if you search for it you'll find it.

With your terminal open, write 'barracks' followed by a space, and then the path of your '.brk' file, which can be absolute, or relative if your terminal is already focused on a folder/directory.

A '.per' file (and a '.ai' file) will be generated in the same location as the '.brk' file, or eventual errors will be reported within the terminal.

Remember you can use the arrow keys to browse previously typed commands if you want to recompile your script again.

Also notice that if you use Visual Studio Code for example, it comes with an integrated terminal window.


To learn how to code in Barracks you can read the [documentation](docs/scripting-guide.md) available under /docs/.
