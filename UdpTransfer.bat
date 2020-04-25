@ECHO OFF
cd projectLearn
javac -d . FileSender.java
javac -d . FileReceiver.java
javac -d . FileInfo.java
javac -d . Filetransfer.java
cd ..
robocopy projectLearn projectLearn1 /e
robocopy projectLearn projectLearn2 /e
START Client1.bat
START Client2.bat