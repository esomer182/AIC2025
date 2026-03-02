# AIC2025 Bot

This repository contains our team’s bot for **AI Coliseum 2025 (AIC2025: Minecraft)**.

We worked on this as a team of 4, splitting different parts of the logic (roles, pathfinding, combat, crafting, coordination, etc.) and then integrating everything into a single coherent bot.

---

## About the Game

AIC2025 is a strategy/resource-management game inspired by Minecraft mechanics.

Each team controls multiple units (called crafters) on a symmetric grid map. The main objective is to **build 4 beacons** before the opponent. You can also win by destroying the opponent’s beds or eliminating all their units.

Some important mechanics:

- The map starts as dirt and gradually generates different resources.
- Units can move, mine, attack, craft tools, and build structures.
- Tools reduce mining cooldown, so crafting them early is important.
- Units have limited vision.
- Units run independently (each one executes its own `run` method).
- Carrying more weight increases cooldown, so inventory management matters.
- Communication between units is limited and must be done via broadcasts.

Because of these constraints, coordination and planning are essential. You need to balance:
- Gathering resources  
- Crafting tools  
- Building infrastructure  
- Defending  
- Attacking  
- Rushing beacons  

More information on [their website](https://www.coliseum.ai/material?lang=en&tournament=aic2025).

---

## Design Approach

We structured the bot in an object-oriented way to keep responsibilities clean and modular.

Main ideas behind the structure:

- `UnitPlayer.java` acts as the entry point for each unit.
- Different behaviors are separated into classes like:
  - `Gatherer`
  - `Attacker`
  - `Tooler`
  - `Pathfinding`
- `Unit.java` provides abstractions around the raw controller.

Instead of putting everything in one giant file, we tried to separate roles and logic into classes with clear responsibilities. This made it easier for different team members to work in parallel.

For example:
- One person focused mainly on pathfinding and movement logic.
- One worked on gathering and economic decisions.
- One focused on combat and micro.
- One worked on coordination, structure placement, and overall integration.

Keeping things modular helped a lot when merging ideas and testing improvements.

---

## Teamwork

This project was developed collaboratively by a team of 4.

We divided responsibilities but constantly discussed strategy decisions:
- When to transition from eco to aggression.
- How many units to dedicate to beacon building.
- How to handle early fights.
- How to encode broadcast messages efficiently.

A big part of the challenge wasn’t just writing code, but designing a consistent strategy that worked across independently running units.

---

## Current State

The bot includes:
- Role-based behavior (gatherers, attackers, etc.)
- Basic coordination via broadcasts
- Pathfinding utilities
- Tool crafting logic
- Structure building logic

There is definitely room for improvement, especially in:
- Smarter combat behavior
- More adaptive role switching
- Better long-term planning

But overall, this repository reflects our final integrated version for the competition.
