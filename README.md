# Nudgery
Ask Yourself

## What Is Nudgery
Nudgery is an Android app that asks you your own questions, on a schedule you choose. You will be able to view the history of each nudge you record, and identify trends over time.

Nudgery contains no ads, and your data stays on your phone (unless you go through the steps to export it).

## What Can You Use Nudgery For?
Nudgery was initially written to help the author track daily migraine status over time, because it is unusually challenging to have good habits when your brain is low-key malfunctioning. The author also thought (while we're here) it would be fun to be able to track whatever the user (you!) thinks would be helpful or interesting to keep track of for a while.

You can set up a Nudge to have your phone ask you anything, on any schedule. Here are some possibilities to get you started:
* Did you have a headache in the last 24 hours?
* Did you dream last night?
* When was the last time you ate a vegetable?
* How annoying was your boss today?
* Did you do anything fun today on purpose?

Additionally, you can set up follow-up questions for specific answers. For instance, if your boss scored 7 or greater on being annoying that day, it could then ask you for some brief notes.

## Setting Up a Nudge
Click on the round "+" button on the main screen. This will bring up a screen that lets you write the main question, and choose what kind of answer you want with the main question (Yes or No, Number, Option (Single), or Option (Multi)). For the Option types, you will be prompted here to add up to 16 selectable answers.

Once the main question is set up, you will be able to add follow-up questions for specific answers or ranges of answers. Follow-up questions can be any of the main question types, plus a freeform Text type.

After the questions are finished, you can edit the schedule for your Nudge to ask the question you just set up. Scheduling options include:
* Daily: Pick a time of day (defaults to noon in your phone's timezone, which will move with your and your phone in case of travel), and the active days of the week
* Weekly: Pick the day of the week, and local time
* Monthly: Pick the day of the month, and local time
* Hourly: Define the hours of the day you want to receive these nudges, and the active days of the week

Save the Nudge. It will appear on the main screen in the list with the rest of your Nudges, indicating the Nudge's name, schedule, next nudge date and time, and whether or not it is enabled. 

Enabled Nudges will send you notifications when it's time to answer your questions. When the notification pops up, clicking on it will take you directly to the question form.

## Viewing Nudges
From the main screen, select the Nudge you would like to view. The Nudge's details will open, with a visualization of the data you have entered so far, editable details of that Nudge, and a raw table of the answer data which can be exported to a CSV/TSV file. There will also be an "Answer Now" button that you can use if you missed a Nudge notification, or if you want to add a data point immediately.

The visualizations available for each main question will differ, based on the main question type.

| Question Type | Available Visualizations |
|---|---|
| YES_NO | Calendar heat map, line graph (daily yes count), column chart |
| NUMBER | Line graph, calendar heat map |
| OPTION_SINGLE | Bar chart, column chart, tag cloud |
| OPTION_MULTI | Bar chart, tag cloud |

The timeframe can be switched between weekly, monthly, yearly, and all-time.


## Editing Nudges
Nudge configuration be edited, except for the base type of the main question. If you edit the main question text or selectable option text, you will be asked if you would prefer to split the Nudge instead of editing in place.
* If you are changing the question enough that the previous answers are no longer accurate answers for the new question, choose to split. The old version of the Nudge and all its related data will be preserved, and the old Nudge will be disabled. The edits will essentially be a new Nudge, which will be enabled going forward.
* If the change to the question is not significant (like a typo change), or if you just don't care (also totally valid), choose not to split the Nudge. The old data will be kept with the new question text, and a note recording the date/time and contents of the edit will be saved with the Nudge.

While you cannot edit Nudge answer data once entered, you can select individual answers and tag them as hidden. Hidden rows no longer appear in the data visualization.
