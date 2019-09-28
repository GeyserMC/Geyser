@echo off
TITLE Manager GEYSER v0.1
cd /d %~dp0
if exist Geyser.jar (
	java -jar Geyser.jar
) else (
	echo Downloading Geyser!

	start https://ci.nukkitx.com/job/Geyser/job/master/lastSuccessfulBuild/artifact/target/Geyser.jar
)
set /p go=Ready? (type "yes" from start Geyser): 
if "%go%" == "yes" (
	start Manager_Geyser.cmd
) else (
	exit
)
