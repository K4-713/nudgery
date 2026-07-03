# Nudgery
Ask Yourself

## What Is Nudgery
Nudgery is an Android app that asks you your own questions, on a schedule you choose. You will be able to view the history of each nudge you record, and identify trends over time.

Nudgery contains no ads, and your data stays on your phone (unless you take the time to move it somewhere else on purpose).

## What Can You Use Nudgery For?
Nudgery was initially written to help the author log daily migraine status over time, because it is unusually challenging to have good habits when your brain is low-key malfunctioning. The author also thought (while we're here) it would be fun to be able to keep an eye on whatever the user (you!) thinks would be helpful or interesting to keep tabs on for a while.

You can set up a Nudge to have your phone ask you anything, on any schedule. Here are some possibilities to get you started:
* Did you have a headache today?
* What did you dream about last night?
* When was the last time you ate a vegetable?
* How annoying was your boss today?
* Did you do anything fun today on purpose?

Additionally, you can set up follow-up questions for specific answers. For instance, if your boss scored 7 or greater on being annoying that day, you could then ask yourself for some brief follow-up notes.

## Setting Up a Nudge
Click on the large "+" graphic or on "Create Your First Nudge" to set up a new Nudge. This will bring up a screen that lets you write the main question, and choose what kind of answer you want with the main question. 

At this stage, you may also rename the Nudge, or leave it alone to go with the default name.

### Question Types
The basic nudge question types are: 
* Yes or No : Simple two-option answer. 
  * With Yes or No questions, you will also get a "Limit to one 'Yes' per day" option. Selecting this will squish whole calendar days of data down to a single "Yes" (if you answered "Yes" at all that day) or "No" in the graphs, instead of counting all the yesses in a day. Raw data will be saved the same way in either case.
* Scale : Whole numbers, 0-10 is the default, and you answer with a slider.
* Any Number : Type any number when asked
* Option (Single) : Pick one option from a list
  * With this question type, you will be prompted here to add anywhere from two to 16 selectable answers.
* Option (Multi) : Pick multiple options from a list
  * Just like the single option type, you will be prompted here to add up to 16 selectable answers.
  * You'll need to add at least two, so you have something to choose between.
* Freeform Text
* Emoji-only answers

Once the main question is set up, you will be able to add one or more optional follow-up questions. Follow-up questions can be set up to always be asked, or (for main question types other than Freeform Text or Emoji) only be asked when you give specific answers to the main question. Follow-up questions can be any of the main question types.

After the questions are finished, you can edit the schedule for your Nudge to ask the question you just set up. Scheduling options include:
* Daily: Pick a time of day (defaults to noon in your phone's timezone, which will move with your and your phone in case of travel), and the active days of the week
* Weekly: Pick the day of the week, and local time
* Monthly: Pick the day of the month, and local time
* Hourly: Pick the time of your first nudge of the day (hour and minute) and the time of your last nudge, plus the active days of the week. Starting the hour and minute you chose for the first nudge, you'll be nudged once an hour until your last nudge time on enabled days. 
  * Note: If your schedule contains a date change (going past midnight), nudges on days you don't have enabled will still fire if that particular run started on an enabled day, and you won't get nudged after midnight on days that are enabled if the previous day wouldn't have started a run.

Save the Nudge. It will appear on the main screen in the list with the rest of your Nudges, indicating the Nudge's name, schedule, next nudge date and time, and whether or not it is enabled. Your nudges can be re-ordered by long-pressing the nudge you want to move, and dragging it to the desired position.

Enabled Nudges will send you notifications when it's time to answer your questions. When the notification pops up, clicking on it will take you directly to the question form.

## Viewing Nudges
From the main screen, select the Nudge you would like to view. The Nudge's details will open, with a visualization of the data you have entered so far, editable details of that Nudge, and a raw table of the answer data which can be exported to a CSV or TSV file, or saved as a full JSON backup (more on this later). There is also an "Answer Now" button that you can use if you missed a Nudge notification, or if you want to add a data point immediately, outside of a scheduled answer.

The raw data shown on the Nudge detail screen displays all nudge responses when expanded. A response consists of a single main question answer, the actual time that answer was received, and answers to any follow-up questions that came along with that main answer (expandable from the main answer).

The visualizations available for each main question will differ, based on the main question type.

| Question Type | Available Visualizations |
|---|---|
| YES_NO | Calendar heat map, line graph (daily yes count), column chart |
| SCALE | Line graph, calendar heat map (daily average) |
| NUMBER | Line graph, calendar heat map (daily average) |
| OPTION_SINGLE | Bar chart, column chart, packed bubble chart |
| OPTION_MULTI | Bar chart, packed bubble chart |
| TEXT | Packed bubble chart (word and emoji frequency) |
| EMOJI | Packed bubble chart |

All the charts on the Nudge detail page will work with the same data: If you have follow-up questions, those charts and the main question chart all move and scale together, so you know you're always viewing the same data set and timeframe.

Data will load most recent first. To look at earlier data, move a chart or time slider, and all the charts will move together.
* On the time-based charts (the calendar heat map and the line graph), just drag the chart left/right.
* On the count-based charts (bar, column, and packed bubble), drag the slim time strip beneath the chart; the bars and bubbles re-tally for whatever window you slide to.

All-time shows your entire history at once, with nothing to scroll.


## Editing Nudges
Everything about Nudge configuration can be edited, except for the base type of the main question. If you edit the main question text or selectable option text, you will be asked if you would prefer to split the Nudge instead of editing in place.
* If you are changing the question enough that the previous answers are no longer accurate answers for the new question, choose to split. The old version of the Nudge and all its related data will be preserved, and the old Nudge will be disabled. The edits will essentially be a new Nudge, which will be enabled going forward.
* If the change to the question is not significant (like a typo change), or if you just don't care (also totally valid), choose not to split the Nudge. The old data will be kept with the new question text, and a note recording the date/time and contents of the edit will be saved with the Nudge.

While you cannot edit Nudge answer data once entered, you can select individual answers and tag them as hidden. Hidden rows no longer appear in the data visualization.

## Deleting Nudges
To delete a Nudge, go to that Nudge's detail screen and tap the "Delete nudge" button on the bottom of the screen. This will permanently delete the nudge, and all its associated data.

To delete a Nudge's follow-up question, go to the Nudge's detail screen. If you have one or more follow-up questions, tap the icon next to the line saying how many follow-ups you have, and click the trash icon on the same line as the name of the follow-up question you want to delete. Deleting a follow-up will permanently remove that follow-up, and any answers you gave for that particular follow-up question.  

## Nudge Backup and Restore
Nudges can be manually backed up and restored through the data download button on the Nudge detail page, to the right of the main chart. When you back up a Nudge, everything about that Nudge will be saved, including name, question and subquestion text, all answer data, enabled status, and schedule.

To back up a Nudge, go to that nudge's detail page, tap the Download icon to the right of the main chart, and choose the "Back up nudge (JSON)" option. The backup file is named after the Nudge, plus the word "nudge" and the export date in compact form — for example, "Good Dog Sightings-nudge-20260601.json" (a Nudge named entirely with emoji is named after the emoji instead, like "dog-face-nudge-20260601.json"). You will then be able to send the backup file wherever you'd like: Files, Email, Drive, etc.

To back up every Nudge at once, open Settings and tap "Back Up All Nudges". This produces a single ZIP archive (named like "nudges-20260601.zip") containing one JSON file per Nudge, each named after its Nudge.

To restore, tap the Settings icon on the main screen, tap "Import from Backup", and choose either a single Nudge's JSON backup or a full backup ZIP (you may have to move it to your device first). A single JSON restores that one Nudge; a ZIP restores every Nudge inside it. Restoring re-creates each Nudge with all settings and data intact.

If an incoming Nudge's name matches one you already have, Nudgery asks what to do for that Nudge: **Replace** the existing one, **Import as a copy** (kept under a numbered name so both survive), or **Skip** it. When you're restoring many Nudges at once, you're asked for each collision in turn, and a "Repeat for all" checkbox lets you apply the same choice to the rest of the batch.

If there's a problem importing your Nudge, and we can fix it, you'll be asked if you want to cancel, or to import and fix it. If you decide to fix it, we'll import as much of the Nudge as we can (including all your past answers) and open it in the editor at the spot that needs correcting.

## Settings

To get to the Settings page, tap the Settings (gear) icon from the main page. Here, you can do a number of things:
* Select the Light or Dark theme. The theme will match your system settings by default, but either can be chosen explicitly in the app.
* Make text more bold than usual, which can help with readability.
* Choose a chart palette that works for you, for things like heat map charts. Several options are available to improve accessibility, including Full Spectrum, Blue to Orange, and Purple to Red.
* Alarm permissions diagnostic information. Depending on your specific phone software, we may need a specific permission to set exact notification "alarms", if you want exact nudge notification schedules. This section tells you if the app has all the permissions it needs to work the best, and how to allow it if you haven't (and want to).
* Back up all of your Nudges at once into a single ZIP archive.
* Import from a backup — either a single Nudge's JSON or a full backup ZIP — which fully restores everything about each Nudge and its related data at the time the backup was saved.
* If you want to use the Emoji question type, select in-app emoji defaults for skin-tone and gender. These defaults will be applied when we can; Specific emoji don't always have multiple styles, but when they do, we'll default them the way you want. You'll still be able to pick whatever version of any emoji you want when you're answering an Emoji question.

# Fabricated Questions
## What happens to my nudge schedule when I change timezones?
Your nudge will fire on schedule, local to whatever your phone's time is. If you want to be asked a question at 9am, you will be asked at 9am wherever you are.

## What happens if you restore a nudge that already exists?
If there's a name collision, you will be given the opportunity to either replace the existing Nudge, import the Nudge as a copy, or skip it.

## What happens if I set up an hourly nudge to start at 9pm and end at 7am Monday through Friday, and it's Saturday at 1am?
We're thinking of the hourly nudge run as a single unit of multiple questions that only start on days you have enabled, at the first nudge time you have selected. So, you'll get your Saturday 1am question, because it's part of a run that was scheduled to start on the previous day. Monday 1am is the other side of that: That run would have had to start Sunday, which in this case is not enabled, so: No early Monday morning nudges. 
We see you, night owls. In fact, we probably are you.

## Is this a health app?
No, not exactly. You can ask yourself anything you want using Nudgery, so it's a good bet that there will be questions relating to your mental or physical health. But, Nudgery is as much of a health app as a blank diary is a medical record: It depends completely on who it belongs to, and what they decide to put in it.

## Why won't my Emoji question show me a bar chart?
There are simply too many options to draw if the whole current universe of your phone's emoji are valid responses. If you want a bar chart, set up either a single option or multi-select option main question, and add the smaller set of Emoji you want to answer with as fixed options.

# License
Code written for this project is effectively released into the public domain under a Creative Commons CC0 license.
Original artwork for this project (app icon, banner, alert icon, etc) is released under a Creative Commons CC-BY-SA license, generally meaning the custom artwork can be used or remixed with attribution, and you must use the same license if you make a derivitive work of your own.

The full dedication is in [LICENSE](LICENSE).
