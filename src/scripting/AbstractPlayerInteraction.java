package scripting;

import java.awt.Point;
import java.util.List;

import client.inventory.Equip;
import client.SkillFactory;
import constants.GameConstants;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.MapleQuestStatus;
import client.Skill;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.guild.MapleGuild;
import server.Randomizer;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.maps.MapleMap;
import server.maps.MapleReactor;
import server.maps.MapleMapObject;
import server.maps.SavedLocationType;
import server.maps.Event_DojoAgent;
import server.life.MapleMonster;
import server.life.MapleLifeFactory;
import server.quest.MapleQuest;
import tools.packet.PetPacket;
import client.inventory.MapleInventoryIdentifier;
import client.messages.CommandProcessor;
import constants.ServerConstants;
import handling.world.World;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.maps.MapleMapFactory;
import tools.FileoutputUtil;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.NPCPacket;
import tools.packet.CField.UIPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InfoPacket;

public abstract class AbstractPlayerInteraction {

    private MapleClient c;

    public AbstractPlayerInteraction(final MapleClient c) {
        this.c = c;
    }

    public final MapleClient getClient() {
        return c;
    }

    public final MapleClient getC() {
        return c;
    }

    public MapleCharacter getChar() {
        return getClient().getPlayer();
    }

    public int getOneTimeLog(String bossid) {
        return getPlayer().getOneTimeLog(bossid);
    }

    public void setOneTimeLog(String bossid) {
        getPlayer().setOneTimeLog(bossid);
    }

    public int getBossLog(String bossid) {
        return getPlayer().getBossLog(bossid);
    }

    public void setBossLog(String bossid) {
        getPlayer().setBossLog(bossid);
    }

    public final ChannelServer getChannelServer() {
        return getClient().getChannelServer();
    }

    public final MapleCharacter getPlayer() {
        return getClient().getPlayer();
    }

    public final EventManager getEventManager(final String event) {
        return getClient().getChannelServer().getEventSM().getEventManager(event);
    }

    public final EventInstanceManager getEventInstance() {
        return getClient().getPlayer().getEventInstance();
    }

    public final void warp(final int map) {
        final MapleMap mapz = getWarpMap(map);
        try {
            c.getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
        } catch (Exception e) {
            c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void warp_Instanced(final int map) {
        final MapleMap mapz = getMap_Instanced(map);
        try {
            getClient().getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
        } catch (Exception e) {
            getClient().getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void warp(final int map, final int portal) {
        final MapleMap mapz = getWarpMap(map);
        if (portal != 0 && map == c.getPlayer().getMapId()) { //test
            final Point portalPos = new Point(c.getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0) { //estimation
                c.sendPacket(CField.instantMapWarp((byte) portal)); //until we get packet for far movement, this will do
                c.getPlayer().checkFollow();
                c.getPlayer().getMap().movePlayer(c.getPlayer(), portalPos);
            } else {
                c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public final void warpS(final int map, final int portal) {
        final MapleMap mapz = getWarpMap(map);
        getClient().getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public final void warp(final int map, String portal) {
        final MapleMap mapz = getWarpMap(map);
        if (map == 109060000 || map == 109060002 || map == 109060004) {
            portal = mapz.getSnowballPortal();
        }
        if (map == c.getPlayer().getMapId()) { //test
            final Point portalPos = new Point(c.getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0) { //estimation
                c.getPlayer().checkFollow();
                c.sendPacket(CField.instantMapWarp((byte) c.getPlayer().getMap().getPortal(portal).getId()));
                c.getPlayer().getMap().movePlayer(c.getPlayer(), new Point(c.getPlayer().getMap().getPortal(portal).getPosition()));
            } else {
                c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public final void warpS(final int map, String portal) {
        final MapleMap mapz = getWarpMap(map);
        if (map == 109060000 || map == 109060002 || map == 109060004) {
            portal = mapz.getSnowballPortal();
        }
        getClient().getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public final void warpMap(final int mapid, final String portal) {
        final MapleMap map = getMap(mapid);
        for (MapleCharacter chr : getClient().getPlayer().getMap().getCharactersThreadsafe()) {
            chr.changeMap(map, map.getPortal(portal));
        }
    }

    public final void warpMap(final int mapid, final int portal) {
        final MapleMap map = getMap(mapid);
        for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
            chr.changeMap(map, map.getPortal(portal));
        }
    }

    public final void playPortalSE() {
        c.sendPacket(EffectPacket.showOwnBuffEffect(0, 7, 1, 1));
    }

    private final MapleMap getWarpMap(final int map) {
        return ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(map);
    }

    public final MapleMap getMap() {
        return getClient().getPlayer().getMap();
    }

    public final MapleMap getMap(final int map) {
        return getWarpMap(map);
    }

    public final MapleMap getMap_Instanced(final int map) {
        return getClient().getPlayer().getEventInstance() == null ? getMap(map) : getClient().getPlayer().getEventInstance().getMapInstance(map);
    }

    public void spawnMonster(final int id, final int qty) {
        spawnMob(id, qty, new Point(c.getPlayer().getPosition()));
    }

    public final void spawnMobOnMap(final int id, final int qty, final int x, final int y, final int map) {
        for (int i = 0; i < qty; i++) {
            getMap(map).spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), new Point(x, y));
        }
    }

    public final void spawnMob(final int id, final int qty, final int x, final int y) {
        spawnMob(id, qty, new Point(x, y));
    }

    public final void spawnMob(final int id, final int x, final int y) {
        spawnMob(id, 1, new Point(x, y));
    }

    private void spawnMob(final int id, final int qty, final Point pos) {
        for (int i = 0; i < qty; i++) {
            getClient().getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public final void killMob(int ids) {
        getClient().getPlayer().getMap().killMonster(ids);
    }

    public final void killAllMob() {
        getClient().getPlayer().getMap().killAllMonsters(true);
    }

    public final void addHP(final int delta) {
        getClient().getPlayer().addHP(delta);
    }

    public final int getPlayerStat(final String type) {
        if (type.equals("LVL")) {
            return getClient().getPlayer().getLevel();
        } else if (type.equals("STR")) {
            return getClient().getPlayer().getStat().getStr();
        } else if (type.equals("DEX")) {
            return getClient().getPlayer().getStat().getDex();
        } else if (type.equals("INT")) {
            return getClient().getPlayer().getStat().getInt();
        } else if (type.equals("LUK")) {
            return getClient().getPlayer().getStat().getLuk();
        } else if (type.equals("HP")) {
            return getClient().getPlayer().getStat().getHp();
        } else if (type.equals("MP")) {
            return getClient().getPlayer().getStat().getMp();
        } else if (type.equals("MAXHP")) {
            return getClient().getPlayer().getStat().getMaxHp();
        } else if (type.equals("MAXMP")) {
            return getClient().getPlayer().getStat().getMaxMp();
        } else if (type.equals("RAP")) {
            return getClient().getPlayer().getRemainingAp();
        } else if (type.equals("RSP")) {
            return getClient().getPlayer().getRemainingSp();
        } else if (type.equals("GID")) {
            return getClient().getPlayer().getGuildId();
        } else if (type.equals("GRANK")) {
            return getClient().getPlayer().getGuildRank();
        } else if (type.equals("ARANK")) {
            return getClient().getPlayer().getAllianceRank();
        } else if (type.equals("GM")) {
            return getClient().getPlayer().isGM() ? 1 : 0;
        } else if (type.equals("ADMIN")) {
            return getClient().getPlayer().hasGmLevel(5) ? 1 : 0;
        } else if (type.equals("GENDER")) {
            return getClient().getPlayer().getGender();
        } else if (type.equals("FACE")) {
            return getClient().getPlayer().getFace();
        } else if (type.equals("HAIR")) {
            return getClient().getPlayer().getHair();
        }
        return -1;
    }

    public final String getName() {
        return getClient().getPlayer().getName();
    }

    public final boolean haveItem(final int itemid) {
        return haveItem(itemid, 1);
    }

    public final boolean haveItem(final int itemid, final int quantity) {
        return haveItem(itemid, quantity, false, true);
    }

    public final boolean haveItem(final int itemid, final int quantity, final boolean checkEquipped, final boolean greaterOrEquals) {
        return getClient().getPlayer().haveItem(itemid, quantity, checkEquipped, greaterOrEquals);
    }

    public final boolean canHold() {
        for (int i = 1; i <= 5; i++) {
            if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).getNextFreeSlot() <= -1) {
                return false;
            }
        }
        return true;
    }

    public final boolean canHold(final int itemid) {
        return getClient().getPlayer().getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public final boolean canHold(final int itemid, final int quantity) {
        return MapleInventoryManipulator.checkSpace(c, itemid, quantity, "");
    }

    public final MapleQuestStatus getQuestRecord(final int id) {
        return getClient().getPlayer().getQuestNAdd(MapleQuest.getInstance(id));
    }

    public final byte getQuestStatus(final int id) {
        return getClient().getPlayer().getQuestStatus(id);
    }

    public final boolean isQuestActive(final int id) {
        return getQuestStatus(id) == 1;
    }

    public final boolean isQuestFinished(final int id) {
        return getQuestStatus(id) == 2;
    }

    public final void showQuestMsg(final String msg) {
        c.sendPacket(CWvsContext.showQuestMsg(msg));
    }

    public final void forceStartQuest(final int id, final String data) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, data);
    }

    public final void forceStartQuest(final int id, final int data, final boolean filler) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, filler ? String.valueOf(data) : null);
    }

    public void forceStartQuest(final int id) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, null);
    }

    public void forceCompleteQuest(final int id) {
        MapleQuest.getInstance(id).forceComplete(getPlayer(), 0);
    }

    public void spawnNpc(final int npcId) {
        getClient().getPlayer().getMap().spawnNpc(npcId, getClient().getPlayer().getPosition());
    }

    public final void spawnNpc(final int npcId, final int x, final int y) {
        getClient().getPlayer().getMap().spawnNpc(npcId, new Point(x, y));
    }

    public final void spawnNpc(final int npcId, final Point pos) {
        getClient().getPlayer().getMap().spawnNpc(npcId, pos);
    }

    public final void removeNpc(final int mapid, final int npcId) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).removeNpc(npcId);
    }

    public final void forceStartReactor(final int mapid, final int id) {
        MapleMap map = getClient().getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.forceStartReactor(c);
                break;
            }
        }
    }

    public final void destroyReactor(final int mapid, final int id) {
        MapleMap map = getClient().getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(c);
                break;
            }
        }
    }

    public final void hitReactor(final int mapid, final int id) {
        MapleMap map = getClient().getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(c);
                break;
            }
        }
    }

    public final int getJob() {
        return getClient().getPlayer().getJob();
    }

    public final void gainPotion(final int type, final int amount) {
        getClient().getPlayer().modifyCSPoints(type, amount, true);
    }

    public final int getPotion(final int type) {
        return getClient().getPlayer().getCSPoints(type);
    }

    public final void gainNX(final int amount) {
        gainPotion(1, amount);
    }

    public final int getNX() {
        return getPotion(1);
    }

    public final void gainMaplePoint(final int amount) {
        gainPotion(2, amount);
    }

    public final int getMaplePoint() {
        return getPotion(2);
    }

    public final void gainItemPeriod(final int id, final short quantity, final int period) { //period is in days
        gainItem(id, quantity, false, period, -1, "");
    }

    public final void gainItemPeriod(final int id, final short quantity, final long period, final String owner) { //period is in days
        gainItem(id, quantity, false, period, -1, owner);
    }

    public final void gainItem(final int id, final short quantity) {
        gainItem(id, quantity, false, 0, -1, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats) {
        gainItem(id, quantity, randomStats, 0, -1, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final int slots) {
        gainItem(id, quantity, randomStats, 0, slots, "");
    }

    public final void gainItem(final int id, final short quantity, final long period) {
        gainItem(id, quantity, false, period, -1, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots) {
        gainItem(id, quantity, randomStats, period, slots, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots, final String owner) {
        gainItem(id, quantity, randomStats, period, slots, owner, c);
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots, final String owner, final MapleClient cg) {
        if (quantity >= 0) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleInventoryType type = GameConstants.getInventoryType(id);

            if (!MapleInventoryManipulator.checkSpace(cg, id, quantity, "")) {

                return;
            }
            if (type.equals(MapleInventoryType.EQUIP) && !GameConstants.isThrowingStar(id) && !GameConstants.isBullet(id)) {
                final Equip item = (Equip) (randomStats ? ii.randomizeStats((Equip) ii.getEquipById(id)) : ii.getEquipById(id));
                if (period > 0) {
                    item.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                if (slots > 0) {
                    item.setUpgradeSlots((byte) (item.getUpgradeSlots() + slots));
                }
                if (owner != null) {
                    item.setOwner(owner);
                }
                final String name = ii.getName(id);
                if (id / 10000 == 114 && name != null && name.length() > 0) { //medal
                    final String msg = "你已獲得稱號 <" + name + ">";
                    cg.getPlayer().dropMessage(-1, msg);
                    cg.getPlayer().dropMessage(5, msg);
                }
                MapleInventoryManipulator.addbyItem(cg, item.copy());
            } else {
                final MaplePet pet;
                if (GameConstants.isPet(id)) {
                    pet = MaplePet.createPet(id, MapleInventoryIdentifier.getInstance());
                } else {
                    pet = null;
                }
                MapleInventoryManipulator.addById(cg, id, quantity, owner == null ? "" : owner, null, period, "" + FileoutputUtil.CurrentReadable_Date());
            }
        } else {
            MapleInventoryManipulator.removeById(cg, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
        cg.sendPacket(InfoPacket.getShowItemGain(id, quantity, true));
    }

    public final void changeMusic(final String songName) {
        getPlayer().getMap().broadcastMessage(CField.musicChange(songName));
    }

    public final void worldMessage(final int type, final String message) {
        World.Broadcast.broadcastMessage(CWvsContext.serverNotice(type, message));
    }

    // default playerMessage and mapMessage to use type 5
    public final void playerMessage(final String message) {
        playerMessage(5, message);
    }

    public final void mapMessage(final String message) {
        mapMessage(5, message);
    }

    public final void guildMessage(final String message) {
        guildMessage(5, message);
    }

    public final void playerMessage(final int type, final String message) {
        c.getPlayer().dropMessage(type, message);
    }

    public final void mapMessage(final int type, final String message) {
        c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(type, message));
    }

    public final void guildMessage(final int type, final String message) {
        if (getPlayer().getGuildId() > 0) {
            World.Guild.guildPacket(getPlayer().getGuildId(), CWvsContext.serverNotice(type, message));
        }
    }

    public final MapleGuild getGuild() {
        return getGuild(getPlayer().getGuildId());
    }

    public final MapleGuild getGuild(int guildid) {
        return World.Guild.getGuild(guildid);
    }

    public final MapleParty getParty() {
        return getClient().getPlayer().getParty();
    }

    public final int getCurrentPartyId(int mapid) {
        return getMap(mapid).getCurrentPartyId();
    }

    public final boolean isLeader() {
        if (getParty() == null) {
            return false;
        }
        return getParty().getLeader().getId() == getClient().getPlayer().getId();
    }

    public final boolean isAllPartyMembersAllowedJob(final int job) {
        if (c.getPlayer().getParty() == null) {
            return false;
        }
        for (final MaplePartyCharacter mem : getClient().getPlayer().getParty().getMembers()) {
            if (mem.getJobId() / 100 != job) {
                return false;
            }
        }
        return true;
    }

    public final boolean allMembersHere() {
        if (c.getPlayer().getParty() == null) {
            return false;
        }
        for (final MaplePartyCharacter mem : getClient().getPlayer().getParty().getMembers()) {
            final MapleCharacter chr = getClient().getPlayer().getMap().getCharacterById(mem.getId());
            if (chr == null) {
                return false;
            }
        }
        return true;
    }

    public final void warpParty(final int mapId) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp(mapId, 0);
            return;
        }
        final MapleMap target = getMap(mapId);
        final int cMap = getPlayer().getMapId();

        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public final void warpParty(final int mapId, final int portal) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            if (portal < 0) {
                warp(mapId);
            } else {
                warp(mapId, portal);
            }
            return;
        }
        final boolean rand = portal < 0;
        final MapleMap target = getMap(mapId);
        final int cMap = getPlayer().getMapId();

        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                if (rand) {
                    try {
                        curChar.changeMap(target, target.getPortal(Randomizer.nextInt(target.getPortals().size())));
                    } catch (Exception e) {
                        curChar.changeMap(target, target.getPortal(0));
                    }
                } else {
                    curChar.changeMap(target, target.getPortal(portal));
                }
            }
        }
    }

    public final void warpParty_Instanced(final int mapId) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp_Instanced(mapId);
            return;
        }
        final MapleMap target = getMap_Instanced(mapId);

        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public void gainMeso(int gain) {
        c.getPlayer().gainMeso(gain, true, true);
    }

    public void gainExp(int gain) {
        getClient().getPlayer().gainExp(gain, true, true, true);
    }

    public void gainExpR(int gain) {
        getClient().getPlayer().gainExp(gain * getClient().getChannelServer().getExpRate(), true, true, true);
    }

    public final void givePartyItems(final int id, final short quantity, final List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            if (quantity >= 0) {
                // 20160727
                MapleInventoryManipulator.addById(chr.getClient(), id, quantity, "");
            } else {
                MapleInventoryManipulator.removeById(chr.getClient(), GameConstants.getInventoryType(id), id, -quantity, true, false);
            }
            chr.getClient().sendPacket(InfoPacket.getShowItemGain(id, quantity, true));
        }
    }

    public final void givePartyItems(final int id, final short quantity) {
        givePartyItems(id, quantity, false);
    }

    public final void givePartyItems(final int id, final short quantity, final boolean removeAll) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainItem(id, (short) (removeAll ? -getPlayer().itemQuantity(id) : quantity));
            return;
        }

        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getMap().getCharacterById(chr.getId());
            if (curChar != null) {
                gainItem(id, (short) (removeAll ? -curChar.itemQuantity(id) : quantity), false, 0, 0, "", curChar.getClient());
            }
        }
    }

    public final void givePartyExp(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.gainExp(amount * getClient().getChannelServer().getExpRate(), true, true, true);
        }
    }

    public final void givePartyExp(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainExp(amount * getClient().getChannelServer().getExpRate());
            return;
        }
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getMap().getCharacterById(chr.getId());
            if (curChar != null) {
                curChar.gainExp(amount * getClient().getChannelServer().getExpRate(), true, true, true);
            }
        }
    }

    public final void givePartyNX(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.modifyCSPoints(1, amount, true);
        }
    }

    public final void givePartyNX(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainNX(amount);
            return;
        }
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getMap().getCharacterById(chr.getId());
            if (curChar != null) {
                curChar.modifyCSPoints(1, amount, true);
            }
        }
    }

    public final void endPartyQuest(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.endPartyQuest(amount);
        }
    }

    public final void endPartyQuest(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            getPlayer().endPartyQuest(amount);
            return;
        }
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getMap().getCharacterById(chr.getId());
            if (curChar != null) {
                curChar.endPartyQuest(amount);
            }
        }
    }

    public final void removeFromParty(final int id, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            final int possesed = chr.getInventory(GameConstants.getInventoryType(id)).countById(id);
            if (possesed > 0) {
                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(id), id, possesed, true, false);
                chr.getClient().sendPacket(InfoPacket.getShowItemGain(id, (short) -possesed, true));
            }
        }
    }

    public final void removeFromParty(final int id) {
        givePartyItems(id, (short) 0, true);
    }

    public final void useSkill(final int skill, final int level) {
        if (level <= 0) {
            return;
        }
        SkillFactory.getSkill(skill).getEffect(level).applyTo(c.getPlayer());
    }

    public final void useItem(final int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(c.getPlayer());
        c.sendPacket(InfoPacket.getStatusMsg(id));
    }

    public final void cancelItem(final int id) {
        getClient().getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(id), false, -1);
    }

    public final int getMorphState() {
        return getClient().getPlayer().getMorphState();
    }

    public final void removeAll(final int id) {
        getClient().getPlayer().removeAll(id, true);
    }

    public final void gainCloseness(final int closeness, final int index) {
        final MaplePet pet = getPlayer().getPet(index);
        if (pet != null) {
            pet.setCloseness(pet.getCloseness() + closeness);
            getClient().sendPacket(PetPacket.updatePet(pet, getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
        }
    }

    public final void gainClosenessAll(final int closeness) {
        for (final MaplePet pet : getPlayer().getPets()) {
            if (pet != null) {
                pet.setCloseness(pet.getCloseness() + closeness);
                getClient().sendPacket(PetPacket.updatePet(pet, getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
            }
        }
    }

    public final void resetMap(final int mapid) {
        getMap(mapid).resetFully();
    }

    public final void openNpc(final int id) {
        openNpc(id, null);
    }

    public final void openNpc(final int id, final int mode) {
        openNpc(getClient(), id, mode, null);
    }

    public final void openNpc(final MapleClient cg, final int id) {
        NPCScriptManager.getInstance().dispose(cg);
        openNpc(cg, id, 0, null);
    }

    public final void openNpc(final int id, final String script) {
        openNpc(getClient(), id, script);
    }

    public final void openNpc(final MapleClient cg, final int id, final String script) {
        openNpc(getClient(), id, 0, script);
    }

    public final void openNpc(final MapleClient cg, final int id, final int mode, final String script) {
        cg.removeClickedNPC();
        NPCScriptManager.getInstance().start(cg, id, mode, script);
    }

    public final int getMapId() {
        return getClient().getPlayer().getMapId();
    }

    public final boolean haveMonster(final int mobid) {
        for (MapleMapObject obj : getClient().getPlayer().getMap().getAllMonstersThreadsafe()) {
            final MapleMonster mob = (MapleMonster) obj;
            if (mob.getId() == mobid) {
                return true;
            }
        }
        return false;
    }

    public final int getChannelNumber() {
        return getClient().getChannel();
    }

    public final int getMonsterCount(final int mapid) {
        return getClient().getChannelServer().getMapFactory().getMap(mapid).getNumMonsters();
    }

    public final void teachSkill(final int id, final int level, final byte masterlevel) {
        getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public final void teachSkill(final int id, int level) {
        final Skill skil = SkillFactory.getSkill(id);
        if (getPlayer().getSkillLevel(skil) > level) {
            level = getPlayer().getSkillLevel(skil);
        }
        getPlayer().changeSingleSkillLevel(skil, level, (byte) skil.getMaxLevel());
    }

    public final int getPlayerCount(final int mapid) {
        return getClient().getChannelServer().getMapFactory().getMap(mapid).getCharactersSize();
    }

    public final void dojo_getUp() {
        c.sendPacket(InfoPacket.updateInfoQuest(1207, "pt=1;min=4;belt=1;tuto=1")); //todo
        c.sendPacket(EffectPacket.Mulung_DojoUp2());
        c.sendPacket(CField.instantMapWarp((byte) 6));
    }

    public final boolean dojoAgent_NextMap(final boolean dojo, final boolean fromresting) {
        if (dojo) {
            return Event_DojoAgent.warpNextMap(c.getPlayer(), fromresting, c.getPlayer().getMap());
        }
        return Event_DojoAgent.warpNextMap_Agent(c.getPlayer(), fromresting);
    }

    public final int dojo_getPts() {
        return c.getPlayer().getIntNoRecord(GameConstants.DOJO);
    }

    public final MapleEvent getEvent(final String loc) {
        return getClient().getChannelServer().getEvent(MapleEventType.valueOf(loc));
    }

    public final int getSavedLocation(final String loc) {
        final Integer ret = getClient().getPlayer().getSavedLocation(SavedLocationType.fromString(loc));
        if (ret == null || ret == -1) {
            return 100000000;
        }
        return ret;
    }

    public final void saveLocation(final String loc) {
        getClient().getPlayer().saveLocation(SavedLocationType.fromString(loc));
    }

    public final void saveReturnLocation(final String loc) {
        getClient().getPlayer().saveLocation(SavedLocationType.fromString(loc), getClient().getPlayer().getMap().getReturnMap().getId());
    }

    public final void clearSavedLocation(final String loc) {
        getClient().getPlayer().clearSavedLocation(SavedLocationType.fromString(loc));
    }

    public final void summonMsg(final String msg) {
        if (!c.getPlayer().hasSummon()) {
            playerSummonHint(true);
        }
        c.sendPacket(UIPacket.summonMessage(msg));
    }

    public final void summonMsg(final int type) {
        if (!c.getPlayer().hasSummon()) {
            playerSummonHint(true);
        }
        c.sendPacket(UIPacket.summonMessage(type));
    }

    public final void showInstruction(final String msg, final int width, final int height) {
        c.sendPacket(CField.sendHint(msg, width, height));
    }

    public void showEffect(boolean broadcast, String effect) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(CField.showEffect(effect));
        } else {
            c.sendPacket(CField.showEffect(effect));
        }
    }

    public final void playerSummonHint(final boolean summon) {
        c.getPlayer().setHasSummon(summon);
        c.sendPacket(UIPacket.summonHelper(summon));
    }

    public final String getInfoQuest(final int id) {
        return getClient().getPlayer().getInfoQuest(id);
    }

    public final void updateInfoQuest(final int id, final String data) {
        getClient().getPlayer().updateInfoQuest(id, data);
    }

    public final boolean getEvanIntroState(final String data) {
        return getInfoQuest(22013).equals(data);
    }

    public final void updateEvanIntroState(final String data) {
        updateInfoQuest(22013, data);
    }

    public final void Aran_Start() {
        c.sendPacket(CField.Aran_Start());
    }

    public final void evanTutorial(final String data, final int v1) {
        c.sendPacket(NPCPacket.getEvanTutorial(data));
    }

    public final void AranTutInstructionalBubble(final String data) {
        c.sendPacket(EffectPacket.AranTutInstructionalBalloon(data));
    }

    public final void ShowWZEffect(final String data) {
        c.sendPacket(EffectPacket.AranTutInstructionalBalloon(data));
    }

    public final void showWZEffect(final String data) {
        c.sendPacket(EffectPacket.ShowWZEffect(data));
    }

    public final void EarnTitleMsg(final String data) {
        c.sendPacket(CWvsContext.getTopMsg(data));
    }

    public final void MovieClipIntroUI(final boolean enabled) {
        c.sendPacket(UIPacket.IntroDisableUI(enabled));
        c.sendPacket(UIPacket.IntroLock(enabled));
    }

    public MapleInventoryType getInvType(int i) {
        return MapleInventoryType.getByType((byte) i);
    }

    public String getItemName(final int id) {
        return MapleItemInformationProvider.getInstance().getName(id);
    }

    public void gainPet(int id, String name, int level, int closeness, int fullness) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        gainPet(id, name, level, closeness, fullness, ii.getPetLife(id), ii.getPetFlagInfo(id));
    }

    public void gainPet(int id, String name, int level, int closeness, int fullness, int period) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        gainPet(id, name, level, closeness, fullness, period, ii.getPetFlagInfo(id));
    }

    public void gainPet(int id, String name, int level, int closeness, int fullness, long period, short flags) {
        if (id > 5000200 || id < 5000000) {
            id = 5000000;
        }
        if (level > 30) {
            level = 30;
        }
        if (closeness > 30000) {
            closeness = 30000;
        }
        if (fullness > 100) {
            fullness = 100;
        }
        try {
            MapleInventoryManipulator.addById(c, id, (short) 1, "", MaplePet.createPet(id, name, level, closeness, fullness, MapleInventoryIdentifier.getInstance(), id == 5000054 ? (int) period : 0, flags), 45, "" + FileoutputUtil.CurrentReadable_Date());
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    public void removeSlot(int invType, byte slot, short quantity) {
        MapleInventoryManipulator.removeFromSlot(c, getInvType(invType), slot, quantity, true);
    }

    public void gainGP(final int gp) {
        if (getPlayer().getGuildId() <= 0) {
            return;
        }
        World.Guild.gainGP(getPlayer().getGuildId(), gp); //1 for
    }

    public int getGP() {
        if (getPlayer().getGuildId() <= 0) {
            return 0;
        }
        return World.Guild.getGP(getPlayer().getGuildId()); //1 for
    }

    public void showMapEffect(String path) {
        getClient().sendPacket(CField.MapEff(path));
    }

    public int itemQuantity(int itemid) {
        return getPlayer().itemQuantity(itemid);
    }

    public EventInstanceManager getDisconnected(String event) {
        EventManager em = getEventManager(event);
        if (em == null) {
            return null;
        }
        for (EventInstanceManager eim : em.getInstances()) {
            if (eim.isDisconnected(c.getPlayer()) && eim.getPlayerCount() > 0) {
                return eim;
            }
        }
        return null;
    }

    public boolean isAllReactorState(final int reactorId, final int state) {
        boolean ret = false;
        for (MapleReactor r : getMap().getAllReactorsThreadsafe()) {
            if (r.getReactorId() == reactorId) {
                ret = r.getState() == state;
            }
        }
        return ret;
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void spawnMonster(int id) {
        spawnMonster(id, 1, new Point(getPlayer().getPosition()));
    }

    // summon one monster, remote location
    public void spawnMonster(int id, int x, int y) {
        spawnMonster(id, 1, new Point(x, y));
    }

    // multiple monsters, remote location
    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    // handler for all spawnMonster
    public void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public void sendNPCText(final String text, final int npc) {
        getMap().broadcastMessage(NPCPacket.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
    }

    public void warpAllPlayer(int from, int to) {

        final MapleMap tomap = getMapFactory().getMap(to);
        final MapleMap frommap = getMapFactory().getMap(from);
        List<MapleCharacter> list = frommap.getCharactersThreadsafe();
        if (tomap != null && frommap != null && list != null && frommap.getCharactersSize() > 0) {
            for (MapleMapObject mmo : list) {
                ((MapleCharacter) mmo).changeMap(tomap, tomap.getPortal(0));
            }
        }
    }

    public MapleMapFactory getMapFactory() {
        return getChannelServer().getMapFactory();
    }

    public void enterMTS() {
//        InterServerHandler.EnterCashShop(c, c.getPlayer(), true);
    }

    public int getChannelOnline() {
        return getClient().getChannelServer().getConnectedClients();
    }

    public int getTotalOnline() {
        return ChannelServer.getAllInstances().stream().map((cserv) -> cserv.getConnectedClients()).reduce(0, Integer::sum);
    }

    public int getMP() {
        return getPlayer().getMP();
    }

    public void setMP(int x) {
        getPlayer().setMP(x);
    }

    public int save(boolean dc, boolean fromcs) {
//        try {
//            return getPlayer().saveToDB(dc, fromcs);
//        } catch (UnsupportedOperationException ex) {
//        }
        return 0;
    }

    public void save() {
        save(false, false);
    }

    public boolean hasSquadByMap() {
        return getPlayer().getMap().getSquadByMap() != null;
    }

    public boolean hasEventInstance() {
        return getPlayer().getEventInstance() != null;
    }

    public boolean hasEMByMap() {
        return getPlayer().getMap().getEMByMap() != null;
    }

    public void processCommand(String line) {
        CommandProcessor.processCommand(getClient(), line, ServerConstants.CommandType.NORMAL);
    }

    public void warpPlayer(int from, int to) {
        final MapleMap mapto = c.getChannelServer().getMapFactory().getMap(to);
        final MapleMap mapfrom = c.getChannelServer().getMapFactory().getMap(from);
        for (MapleCharacter chr : mapfrom.getCharactersThreadsafe()) {
            chr.changeMap(mapto, mapto.getPortal(0));
        }
    }
}
