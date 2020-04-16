@ECHO OFF
cd projectLearn
javac -d . FileTransferSystem.java
javac -d . FileDataBase.java
javac -d . peerClient.java
javac -d . peerServer.java
javac -d . MyServerClientHandler.java
javac -d . MyClient.java
javac -d . MyServer.java
cd ..
robocopy projectLearn projectLearn1 /e
robocopy projectLearn projectLearn2 /e
START myserver.bat
START myclient1.bat
START myclient2.bat

