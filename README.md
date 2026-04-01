![Item Signs](https://imgur.com/s81Fczu.png)

![](https://img.shields.io/badge/Loader-Fabric-%23313e51?style=for-the-badge)
![](https://img.shields.io/badge/MC-26.1--26.1.1%20|%201.21-%23313e51?style=for-the-badge)
![](https://img.shields.io/badge/Side-Client%20&%20Server-%23313e51?style=for-the-badge)

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/item-signs?style=flat&logo=modrinth&color=00AF5C)](https://modrinth.com/mod/item-signs)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1501513?style=flat&logo=curseforge&color=F16436)](https://www.curseforge.com/minecraft/mc-mods/item-signs)
[![GitHub Repo stars](https://img.shields.io/github/stars/Roundaround/mc-fabric-item-signs?style=flat&logo=github)](https://github.com/Roundaround/mc-fabric-item-signs)

[![Support me on Ko-fi](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/donate/kofi-singular-alt_vector.svg)](https://ko-fi.com/roundaround)

---

Place items into signs the same way you can with item frames! Works for signs of all materials, including both hanging and freestanding varieties!

![Screenshot](https://cdn.modrinth.com/data/dwFQ8OlO/images/24522cef8dd0bf8046be82f6a384c625d70e915e.png)

## Compatibility

### Enhanced Block Entities [🔗](https://modrinth.com/mod/ebe)

This mod comes with a compatibility hook for Enhanced Block Entities, disabling its "Enhanced Signs" configuration option, as it takes away the ability to render the mounted items. If the hook ever doesn't work automatically and your items are invisible, check EBE's configuration and make sure it is leaving signs alone!

### KetKet's Better Hanging Signs [🔗](https://modrinth.com/datapack/better-hanging-signs)

If you're coming from KetKet's Better Hanging Signs datapack, you might be pleased to hear it works perfectly fine next to this mod. _More importantly_, if you want to migrate off the datapack to use this mod exclusively, (as of v1.1.0) this mod will detect the absence of the datapack and clean up after it automatically! If a sign still exists and you haven't placed items on it via this mod yet, it'll transfer the items directly onto the mod while it cleans up the invisible entities spawned by the datapack. If it can't find a sign or there aren't enough slots available, it'll drop the old items on the floor and notify you in chat!
