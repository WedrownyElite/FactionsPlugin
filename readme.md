# EclipseFactions
*A modern OG-style Factions plugin built for 1.16.5 with 1.8-style combat.*

EclipseFactions is a lightweight yet feature-rich factions plugin designed for players who want the nostalgic Factions experience — without the bloat.  
It keeps the core mechanics you know and love while adding modern conveniences and customization through GUIs.

---

## ✨ Features

### ✅ Implemented

#### Core Factions System
- Create & disband factions via commands or GUI
- Public factions (open join) & private factions (invite-only)
- Invite players via `/f invite` or GUI
- Join & leave factions easily
- Owner leadership transfer

#### Claiming & Power
- Chunk-based claiming (2 power per claimed chunk)
- 1 power refunded when unclaiming land
- Starting power: **10**
- Power gain: **+2 power/hour played**
- Power cap: **20**
- Homes must be set inside claimed land — unclaiming removes the home

#### Relations System
- Set relations with:
  - `/f ally`
  - `/f truce`
  - `/f enemy`
  - `/f neutral`
- Player name suffixes show relation status (ally, truce, enemy, member)
- Private & public relation settings

#### Ranks & Permissions
- 4 default ranks: Recruit, Member, Mod, Admin
- Toggleable permissions per rank (e.g., container access, `/f home`, `/f warp`, etc.)
- Relation-based permissions (enemy, neutral, truce, ally)
- Hierarchical editing rules — cannot edit same/higher rank

#### Homes & Warps
- `/f sethome` and `/f home` commands
- 1 faction warp by default (upgradeable)
- GUI warp list with teleport buttons

#### Faction Bank
- `/f bank` command
- Withdraw & deposit money
- Bank transaction logs (player, amount, date/time)

### Faction Discord Integration
- `/f setdiscord` command to store faction Discord invite links (requires permissions)
- `/f discord` command to display faction Discord invite links (requires permissions)
- `/f unsetdiscord` command to remove faction Discord invite links (requires permissions)

#### GUI Management
- Faction creation
- Permission management
- Claim management & display
- Member management (kick, promote, demote)
- Relation management
- Faction info display
- Toggle visibility (public/private)
- Home & warp display + teleport buttons
- Bank interface with deposit/withdraw options

---

## 🛠 Planned Features

### Faction Discord Integration
- Clickable link in faction info

### Faction Announcements
- Send announcements to all members
- Persistent announcements viewable via `/f announcements` or GUI tab
- Pinned announcements for important updates

### Faction Upkeep
- Daily money cost to maintain claims
- Failure to pay results in gradual unclaim of land
- Amount scales with number of claims

### Faction Leveling
- Earn faction XP from:
  - Player activity (time online, killing mobs, PvP)
  - Claiming land
  - Completing faction events/challenges
- Leveling unlocks:
  - Increased max power cap
  - Faster power regeneration
  - More faction warps
  - Reduced upkeep costs

---

## 💡 Upgrade Ideas

Upgrades can be unlocked via **Faction Leveling** or purchased with in-game money:

| Upgrade            | Effect |
|--------------------|--------|
| Max Power Boost    | +2 max power per level (up to a configurable cap) |
| Power Regen Boost  | +0.5 to +1 power/hour faster regen |
| Extra Warps        | +1 warp per upgrade (up to configurable cap) |
| Upkeep Discount    | Reduces daily upkeep by 5–10% per upgrade |

---

## 📜 Commands

| Command | Description |
|---------|-------------|
| `/f create <name>` | Create a new faction |
| `/f disband` | Disband your faction |
| `/f invite <player>` | Invite a player |
| `/f join <name>` | Join a faction |
| `/f claim` | Claim the chunk you’re standing in |
| `/f unclaim` | Unclaim the chunk you’re standing in |
| `/f home` | Teleport to faction home |
| `/f sethome` | Set faction home |
| `/f warp` | Teleport to a faction warp |
| `/f setwarp <name>` | Set a faction warp |
| `/f ally/truce/enemy/neutral <faction>` | Manage faction relations |
| `/f bank` | Open faction bank menu |
| `/f announcements` | View faction announcements (Coming soon) |
| `/f discord` | View and set the faction's Discord link (Coming soon) |

---

## ⚙ Permissions
All permissions are configurable via the rank management GUI.

---

## 🔮 Goals
EclipseFactions will remain lightweight, simple to configure, and focused on the **classic competitive factions experience** while offering enough customization to feel fresh for modern players.
