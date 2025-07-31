<p align="center">
	<img src="logo.png" width="376" height="128" alt="Winlator Logo" />
</p>

# Steamlator

🏗 Based on Winlator Just Bionic 1.5.2

Steam is already built into the APK – when you launch the container, the Steam launcher will open automatically.
Uses special launch arguments for maximum compatibility.
Verified: Steam works on devices with Helio G99 and Snapdragon 665
(if your processor is more powerful, it will obviously work too).

Winlator is an Android application that lets you to run Windows (x86_64) applications with Proton and Box86/Box64.

# Installation

1. Download and install the APK (Steamlator 1.5.2) from [GitHub Releases](https://github.com/slaker222/steamlator/releases)
2. Launch the app and wait for the installation process to finish

# Snapdragon 695



https://github.com/user-attachments/assets/35036d22-6cba-445b-ad2a-5b197092dd64



----

#⚠️ Issues & Solutions

🔄 Endless Steam Loading
Solution:
Close the container using the Exit button and reopen it – the endless loading issue will be gone.
🛑 Game says your hardware is too weak

Solution:
Replace Wrapper with Turnip (Adreno only).
❌ DXVK doesn’t work on Wrapper

Solution:
Use DXVK below version 2.0 (other versions are for Turnip).


# Credits and Third-party apps
- GLIBC Patches by [Termux Pacman](https://github.com/termux-pacman/glibc-packages)
- Wine ([winehq.org](https://www.winehq.org/))
- Box86/Box64 by [ptitseb](https://github.com/ptitSeb)
- Mesa (Turnip/Zink/VirGL) ([mesa3d.org](https://www.mesa3d.org))
- DXVK ([github.com/doitsujin/dxvk](https://github.com/doitsujin/dxvk))
- VKD3D ([gitlab.winehq.org/wine/vkd3d](https://gitlab.winehq.org/wine/vkd3d))
- CNC DDraw ([github.com/FunkyFr3sh/cnc-ddraw](https://github.com/FunkyFr3sh/cnc-ddraw))

Special thanks to all the developers involved in these projects.<br>
Thank you to all the people who believe in this project.

if you want to build apk you need to download [MEGA](https://mega.nz/folder/X1Yw1YDa#g8GBelGf20UROVbZRq31rQ)
container_pattern_arm64ec.tzst
container_pattern_common.tzst
container_pattern_x86_64.tzst
imagefs.txz
proton-9.0-x86_64_container_pattern.tzst
proton-9.0-x86_64.txz 
