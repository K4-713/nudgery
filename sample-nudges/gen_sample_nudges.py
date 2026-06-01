#!/usr/bin/env python3
"""Generate importable Nudgery JSON backups with ~500 days of cute fake data,
one per basic question type. Format mirrors ExportAnswersUseCase.buildJsonExport
and is consumed by NudgeBackupParser.

Run with no arguments; files are written next to this script. Import them via
Settings -> "Import Nudge from Backup" on the device."""

import json
import math
import os
import random
from datetime import datetime, timedelta, timezone

random.seed(1734)  # deterministic output

OUT_DIR = os.path.dirname(os.path.abspath(__file__))
DAYS = 500
END = datetime(2026, 6, 1, 12, 0, 0, tzinfo=timezone.utc)
ALL_DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]
SKIP_CHANCE = 0.08  # leave some days with no data so empty heat-map cells show

def iso(dt):
    return dt.strftime("%Y-%m-%dT%H:%M:%SZ")

def each_day():
    """Yields (scheduled_at, answered_at) for each non-skipped day, oldest first."""
    for offset in range(DAYS - 1, -1, -1):
        if random.random() < SKIP_CHANCE:
            continue
        scheduled = END - timedelta(days=offset)
        answered = scheduled + timedelta(minutes=random.randint(1, 230))
        yield scheduled, answered

def seasonal(scheduled, amplitude, midpoint, period_days=365.0, phase=0.0):
    """Gentle sine wave over the year for natural-looking trends."""
    day_of_year = scheduled.timetuple().tm_yday
    return midpoint + amplitude * math.sin(2 * math.pi * (day_of_year / period_days) + phase)

def base_doc(name, questions, answers):
    return {
        "nudge": {"name": name, "isEnabled": False},
        "schedule": {
            "type": "DAILY",
            "timeOfDay": "12:00",
            "activeDaysOfWeek": ALL_DAYS,
            "dayOfMonth": None,
            "activeHours": None,
        },
        "questions": questions,
        "answers": answers,
    }

def write(filename, doc):
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, filename)
    with open(path, "w") as f:
        json.dump(doc, f, indent=2)
    print(f"wrote {path}  ({len(doc['answers'])} answers)")


# 1) YES_NO  + TEXT follow-up riding along
def gen_yes_no():
    dog_notes = [
        "a noble golden", "tiny zoomies", "ears too big", "very polite boy",
        "fluffiest cloud", "smol and brave", "majestic floof", "wiggly hello",
        "sleepy sidewalk pup", "extremely round", "spotted gentleman",
        "happy tail helicopter", "good hat, good dog", "carried a big stick",
        "weighs a ton", "absolute unit", "dingdoggler",
    ]
    questions = [
        {"orderIndex": 0, "text": "Did you spot a good dog today?", "type": "YES_NO"},
        {"orderIndex": 1, "text": "What was the dog situation?", "type": "TEXT",
         "triggerOperator": "EQ", "triggerAnswerValue": "YES"},
    ]
    answers = []
    for scheduled, answered in each_day():
        # More good dogs on weekends; generally a happy world.
        p_yes = 0.80 if scheduled.weekday() >= 5 else 0.68
        said_yes = random.random() < p_yes
        answers.append({"questionOrderIndex": 0, "value": "YES" if said_yes else "NO",
                        "scheduledAt": iso(scheduled), "answeredAt": iso(answered)})
        if said_yes and random.random() < 0.7:
            answers.append({"questionOrderIndex": 1, "value": random.choice(dog_notes),
                            "scheduledAt": iso(scheduled),
                            "answeredAt": iso(answered + timedelta(minutes=1))})
    write("nudgery-good-dogs.json", base_doc("Good Dog Sightings", questions, answers))


# 2) SCALE (1-10)
def gen_scale():
    questions = [
        {"orderIndex": 0, "text": "How majestic was the sky today?", "type": "SCALE",
         "scaleMin": 1, "scaleMax": 10},
    ]
    answers = []
    for scheduled, answered in each_day():
        base = seasonal(scheduled, amplitude=2.2, midpoint=6.5, phase=-1.2)
        val = round(base + random.gauss(0, 1.3))
        val = max(1, min(10, val))
        answers.append({"questionOrderIndex": 0, "value": str(val),
                        "scheduledAt": iso(scheduled), "answeredAt": iso(answered)})
    write("nudgery-sky-majesty.json", base_doc("Sky Majesty Meter", questions, answers))


# 3) NUMBER
def gen_number():
    questions = [
        {"orderIndex": 0, "text": "How many cups of tea did you enjoy?", "type": "NUMBER"},
    ]
    answers = []
    for scheduled, answered in each_day():
        # Cozier (more tea) in winter and on weekends.
        base = seasonal(scheduled, amplitude=1.4, midpoint=2.6, phase=math.pi)  # peak in winter
        if scheduled.weekday() >= 5:
            base += 0.8
        val = round(base + random.gauss(0, 0.9))
        val = max(0, min(7, val))
        answers.append({"questionOrderIndex": 0, "value": str(val),
                        "scheduledAt": iso(scheduled), "answeredAt": iso(answered)})
    write("nudgery-tea-count.json", base_doc("Daily Tea Count", questions, answers))


# 4) OPTION_SINGLE
def gen_option_single():
    options = ["Warm cookie", "Crunchy apple", "Cheese cube", "Dark chocolate", "Buttered toast"]
    weights = [0.30, 0.18, 0.17, 0.22, 0.13]
    questions = [
        {"orderIndex": 0, "text": "Which snack won the day?", "type": "OPTION_SINGLE",
         "options": options},
    ]
    answers = []
    for scheduled, answered in each_day():
        choice = random.choices(options, weights=weights, k=1)[0]
        answers.append({"questionOrderIndex": 0, "value": choice,
                        "scheduledAt": iso(scheduled), "answeredAt": iso(answered)})
    write("nudgery-snack-champion.json", base_doc("Today's Snack Champion", questions, answers))


# 5) OPTION_MULTI
def gen_option_multi():
    options = ["Hot tea", "Soft blanket", "Good book", "Candlelight", "Lo-fi music",
               "Afternoon nap", "Warm socks"]
    weights = [0.7, 0.6, 0.45, 0.35, 0.5, 0.3, 0.4]
    questions = [
        {"orderIndex": 0, "text": "Which cozy things did you enjoy today?", "type": "OPTION_MULTI",
         "options": options},
    ]
    answers = []
    for scheduled, answered in each_day():
        picked = [opt for opt, w in zip(options, weights) if random.random() < w]
        if not picked:
            picked = [random.choice(options)]
        # Backup format joins multi-select answer texts with ", "
        answers.append({"questionOrderIndex": 0, "value": ", ".join(picked),
                        "scheduledAt": iso(scheduled), "answeredAt": iso(answered)})
    write("nudgery-cozy-things.json", base_doc("Cozy Things Enjoyed", questions, answers))


if __name__ == "__main__":
    gen_yes_no()
    gen_scale()
    gen_number()
    gen_option_single()
    gen_option_multi()
