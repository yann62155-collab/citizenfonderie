# Citizen Fonderie — fours de forgeron (NeoForge 21.1.215 / MC 1.21.1 / Java 21)

## Les trois fours

| Four | Blocs | Taille (largeur × hauteur × profondeur) | Minerais en même temps |
|---|---|---|---|
| Four de forgeron simple | 2 | 1 × 2 × 1 | 2 |
| Four de forgeron apprenti | 4 | 2 × 2 × 1 | 4 |
| Four de maître forgeron | 8 | 2 × 2 × 2 | 8 |

- On pose le four comme un seul bloc : il se construit tout seul vers le haut, la droite et l'arrière.
  S'il n'y a pas assez de place, il n'est pas posé et un message s'affiche.
- Clic droit sur n'importe quel bloc du four = ouvre le GUI.
- Chaque colonne du GUI = 1 minerai qui fond indépendamment (minerai en haut, lingot en bas).
  On peut mettre des minerais différents dans chaque colonne.
- Un seul emplacement de combustible pour tout le four. Le feu s'use d'une unité par tick **et par
  minerai en fonte** : 8 minerais à la fois consomment autant que 8 fours vanilla (pas de charbon gratuit).
- Utilise les recettes de cuisson du jeu, donc marche aussi avec les minerais d'autres mods
  (s'ils sont dans les tags `c:ores` / `c:raw_materials`).
- L'expérience est donnée quand on retire les lingots.
- Casser un bloc du four casse toute la structure : 1 seul item du four est lâché + tout le contenu.
- Entonnoirs : par le dessus → minerais, par les côtés → combustible, par le dessous → lingots.

## Fours vanilla bloqués

Par défaut, le four, le haut fourneau et le fumoir vanilla ne fondent plus les minerais
(il faut un four de forgeron). Config : `config/citizenfonderie-common.toml`

```toml
bloquerMineraisFoursVanilla = true   # false = les fours vanilla refondent les minerais
accepterToutesCuissons = false       # true = les fours de forgeron cuisent aussi sable, nourriture...
```

La liste des « minerais » est le tag `citizenfonderie:minerais` (modifiable par datapack).

## Recettes

- **Simple** : 6 pierres taillées (cobblestone) + 2 fours — sans lingot, puisque les minerais ne fondent plus ailleurs.
  ```
  C C C
  F . F
  C C C
  ```
- **Apprenti** : 2 fours simples + 5 briques (bloc) + 1 lingot de fer
  ```
  B I B
  S . S
  B B B
  ```
- **Maître** : 2 fours apprentis + 3 briques de pierre noire polie + 2 lingots d'or + 1 bloc de fer
  ```
  G N G
  A . A
  N I N
  ```

## Compiler

```
gradlew build
```
Le JAR est dans `build/libs/citizenfonderie-1.1.0.jar`. À mettre **côté serveur et côté joueurs**
(il y a des blocs et un GUI).

Si Gradle ne trouve pas le plugin `net.neoforged.moddev` version `2.0.78`, remplace ce numéro dans
`build.gradle` par celui qui marche dans tes autres mods.

## Équipement de samouraï en jade

Se forge avec le **Lingot de Jade** de Citizen Guilds (le minerai de jade ne s'obtient que dans les donjons).
Le minerai de jade se fond maintenant au **four de forgeron** (les fours vanilla le refusent).
Les recettes ne se chargent que si Citizen Guilds est installé.

| Objet | Recette (J = lingot de jade, G = lingot d'or, S = bâton) | Stats |
|---|---|---|
| Katana de jade | `  J` / `GJ ` / `S  ` | 8 dégâts, vitesse 1,8, 2300 utilisations |
| Kabuto de jade (casque 3D de samouraï) | `JGJ` / `J J` | 3 armure |
| Plastron de samouraï en jade | `J J` / `JJJ` / `JJJ` | 8 armure |
| Jambières de samouraï en jade | `JJJ` / `J J` / `J J` | 6 armure |
| Bottes de samouraï en jade | `J J` / `J J` | 3 armure |

Armure : robustesse 3 par pièce, résiste au feu et à la lave (comme la netherite).
