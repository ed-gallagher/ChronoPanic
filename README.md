ChronoPanic!!
=============

A minigame-based alarm clock app for Android

Created by Ed Gallagher and Carolyn Molina for ECE 5480 (Android Mobile Development), Villanova University, Fall 2013.

## Overview
Chrono Panic!! is an alarm app that uses mini games to both to wake up and entertain the user. When the alarm goes off, the user must complete a series of mini games to turn off the alarm. Currently, there are three mini games that the user must complete. The first mini game requires the user to input the color pattern that they see displayed on the traffic lights using the buttons to the right. The second mini game is similar to the first where the user selects drinks to match the pattern shown. The third mini game requires the user to poke a panda until it wakes up.

Possible future additions to this app include more games and incorporating repeating alarms for the user. Currently, the user can only add alarms that only play once but they cannot set it so it repeats on certain days. Another optional goal is to add user-defined sounds so they can choose their alarm tone instead of the default ~~chiptune~~ disco music that plays.

Application components include the following: activities, services, broadcast receivers, content providers, and threads. Advanced features include the use of SQLite and Still images for the games. AlarmManager, a system service, was used to trigger the alarms.

## Music License
"Who Likes to Party" Kevin MacLeod (incompetech.com)
Licensed under Creative Commons: By Attribution 3.0
<a href="http://creativecommons.org/licenses/by/3.0/" target="_blank">http://creativecommons.org/licenses/by/3.0/</a>

