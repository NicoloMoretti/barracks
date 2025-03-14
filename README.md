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

1. [Download](https://github.com/NicoloMoretti/barracks/releases/tag/v1.0.0) the Windows Barracks zip file.
2. Extract the ZIP file. You should see three files inside the extracted folder.
3. Right-click the 'install.bat' file and select "Run as administrator".

If it worked, you are done.

You can try in the cmd:
```text
barracks --version
```
If you see the version of Barracks displayed, it's set up correctly!

#### Automatic installation for **Linux**:

1. [Download](https://github.com/NicoloMoretti/barracks/releases/tag/v1.0.0) the Linux Barracks zip file.
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

First, create a file ending with a '.brk' extension. (If you are on Windows, be careful to not get something like '.brk.txt')

Then you can open a cmd (Windows) / terminal (linux) window, which is just an application that comes by default with your operative system, if you search for it you'll find it.

With your terminal open, write 'barracks' followed by a space, and then the path of your '.brk' file, which can be absolute, or relative if your terminal is already focused on a folder/directory.

Here's an example of me compiling a file called 'test.brk' placed in my Downloads folder:

Windows absolute path:

<img src="https://github.com/user-attachments/assets/6e1e76a4-cf63-41ce-86b0-d29e91dc5e3e" width="45%" />

Windows relative path:

<img src="https://github.com/user-attachments/assets/734c1e16-cc73-489e-b6c2-66305d4fc63e" width="40%" />


Linux absolute path:

<img src="https://github.com/user-attachments/assets/f7b7f2f9-95dc-4d0e-9b13-14b2b4d9c06f" width="60%" />

Linux relative path:

<img src="https://github.com/user-attachments/assets/bb5a9db5-17fa-4a82-a23f-5e8fcee285bd" width="45%" />


Hit enter and a '.per' file (and a '.ai' file) will be generated in the same location as the '.brk' file, with the same name, or eventual errors will be reported within the terminal.

Remember you can use the arrow keys to browse previously typed commands if you want to recompile your script again, and 'tab' to autocomplete words in the terminal.

Also note that if you use Visual Studio Code, for example, it comes with an integrated terminal window.


To learn how to code in Barracks you can read the [scripting guide](docs/scripting-guide.md) available under /docs/.
