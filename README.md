# Barracks

## What it's about

Barracks is an alternative programming language to script bots/AIs for Age Of Empires 2 DE.
It aims to provide an additional layer of abstraction compared to regular '.per' scripting.
The focus is to make it easier to read/write code compared to writing '.per' files directly, while staying familiar and intuitive.
Code written in Barracks is automatically compiled down to .per files through the Barracks compiler.

## How to install it

The compiler is essentially a Java application, which can be downloaded directly (or compiled from source if preferred).
There isn't a graphic interface, you are supposed to interact with it through the terminal, by executing it with a '.brk' file path as an argument.
A '.per' will be generated in the same location as the '.brk' file, or eventual errors will be reported within the terminal.

### Automatic installation instructions:

#### Automatic installation for **Linux**:

1. Downalod "Barracks_installation_Linux.zip".
2. Extract the ZIP file. You should see three files inside the extracted folder.
3. Open a terminal in the extracted folder:
  - Right-click an empty space in the folder.
  - Select "Open in Terminal" from the dropdown menu.
5. Run the following commands in the terminal:

```text
chmod +x install.sh
sudo ./install.sh
```

If it worked, you are done.


### Manual installation instructions:

To launch it, you should have an up to date Java version installed on your PC, if not you can download it from the Oracle site for example (select your right OS)

https://www.oracle.com/java/technologies/downloads/#jdk23-windows

If you already have Java, once you download the barracks.jar file, that's already enough to use the compiler. Just launch it through the terminal/cmd by doing "java -jar barracks.jar" in the folder with barracks.jar in.

However, that would be a bit inconvenient because you would have to type in a long command (java -jar barracks.jar), so I made a support bat/sh file to launch instead, that will in turn call the .jar for you, you just need to type "bar", press 'tab' on your keyboard, and your system will most likely autocomplete the command for you.

Finally, if you also manage to get both files under PATH, you will be able to call Barracks from anywhere in your system, just open a console and type "barracks" (or bar... something and autocomplete with tab).


For easy use, I suggest:

#### Manual installation for **Windows**:
1. Download the .jar file.

2. Downalod the 'barrack.bat' file as well.

Now, you can already use it by right clicking the folder where they are saved, opening a cmd there, typing something like "barr" and pressing 'tab' to autocomplete

To check if it worked, type in the cmd, opened in the location where the files are saved:

```text
barracks --version
```

If you see the version of Barracks displayed, it's set up correctly!

However, like this you will need to have both files wherever you plan to save script/launch your CMD from, so I would suggest you to get them under PATH somehow. If you feel spartan just drop them in System32 and that will suffice.

After you are done try again:

```text
barracks --version
```

If it worked, you are done.

#### Manual installation for **Linux**:
1. Download the .jar file.

2. Downalod the 'barracks' bash file as well.

3. In the folder where you saved both of them, to put then under PATH quickly, run:
```text
sudo mv ./barracks.jar /usr/bin/barracks.jar
sudo mv ./barracks /usr/bin/barracks
```

So they will end up in /usr/bin/ which is under PATH

To check if it worked, type in the terminal:

```text
barracks --version
```

If you see the version of Barracks displayed, it's set up correctly!


## Getting started

Once you create a file with a .brk file extension, and you have the compiler installed, you just need to open a terminal window and launch Barracks with your .brk file path as argument.

To learn how to code in Barracks you can consult the [documentation](docs/scripting-guide.md) available under /docs/.
