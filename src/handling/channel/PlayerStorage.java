package handling.channel;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import client.MapleCharacterUtil;
import client.MapleCharacter;

import handling.world.CharacterTransfer;
import handling.world.CheaterData;
import handling.world.World;
import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import server.Timer.PingTimer;

public class PlayerStorage {

    private final ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();
    private final Lock rL = mutex.readLock(), wL = mutex.writeLock();
    private final ReentrantReadWriteLock mutex2 = new ReentrantReadWriteLock();
    private final Lock rL2 = mutex2.readLock(), wL2 = mutex2.writeLock();
    private final Map<String, MapleCharacter> nameToChar = new HashMap<>();
    private final Map<Integer, MapleCharacter> idToChar = new HashMap<>();
    private final Map<Integer, CharacterTransfer> PendingCharacter = new HashMap<>();
    private int channel;

    public PlayerStorage(int channel) {
        this.channel = channel;
        // Prune once every 15 minutes
        PingTimer.getInstance().register(new PersistingTask(), 60000);
    }

    public final ArrayList<MapleCharacter> getAllCharacters() {
        rL.lock();
        try {
            return new ArrayList<>(idToChar.values());
        } finally {
            rL.unlock();
        }
    }

    public final List<MapleCharacter> getAllCharactersThreadSafe() {
        List<MapleCharacter> ret = new ArrayList<>();
        try {
            ret.addAll(getAllCharacters());
        } catch (ConcurrentModificationException ex) {

        }
        return ret;
    }

    public final void registerPlayer(final MapleCharacter chr) {
        wL.lock();
        try {
            nameToChar.put(chr.getName().toLowerCase(), chr);
            idToChar.put(chr.getId(), chr);
        } finally {
            wL.unlock();
        }
        World.Find.register(chr.getId(), chr.getName(), channel);
    }

    public final void registerPendingPlayer(final CharacterTransfer chr, final int playerid) {
        wL2.lock();
        try {
            for (CharacterTransfer transfer : PendingCharacter.values()) {
                if (transfer.accountid == chr.accountid) {
                    PendingCharacter.remove(transfer.characterid);
                }
            }
            PendingCharacter.put(playerid, chr);//new Pair(System.currentTimeMillis(), chr));
        } finally {
            wL2.unlock();
        }
    }

    public final void deregisterPlayer(final MapleCharacter chr) {
        wL.lock();
        try {
            nameToChar.remove(chr.getName().toLowerCase());
            idToChar.remove(chr.getId());
        } finally {
            wL.unlock();
        }
        World.Find.forceDeregister(chr.getId(), chr.getName());
    }

    public final void deregisterPlayer(final int idz, final String namez) {
        wL.lock();
        try {
            nameToChar.remove(namez.toLowerCase());
            idToChar.remove(idz);
        } finally {
            wL.unlock();
        }
        World.Find.forceDeregister(idz, namez);
    }

    public final int pendingCharacterSize() {
        return PendingCharacter.size();
    }

    public final void deregisterPendingPlayer(final int charid) {
        wL2.lock();
        try {
            int accountid = 0;
            if (PendingCharacter.containsKey(charid)) {
                accountid = PendingCharacter.get(charid).accountid;
            }
            PendingCharacter.remove(charid);
            if (accountid > 0) {
                for (CharacterTransfer transfer : PendingCharacter.values()) {
                    if (transfer.accountid == accountid) {
                        PendingCharacter.remove(transfer.characterid);
                    }
                }
            }
        } finally {
            wL2.unlock();
        }
    }

    public final CharacterTransfer getPendingCharacter(final int charid) {
        wL2.lock();
        try {
            return PendingCharacter.remove(charid);
        } finally {
            wL2.unlock();
        }
    }

    public final MapleCharacter getCharacterByName(final String name) {
        rL.lock();
        try {
            return nameToChar.get(name.toLowerCase());
        } finally {
            rL.unlock();
        }
    }

    public final MapleCharacter getCharacterById(final int id) {
        rL.lock();
        try {
            return idToChar.get(id);
        } finally {
            rL.unlock();
        }
    }

    public final int getConnectedClients() {
        return idToChar.size();
    }

    public final List<CheaterData> getCheaters() {
        final List<CheaterData> cheaters = new ArrayList<>();

        rL.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getCheatTracker().getPoints() > 0) {
                    cheaters.add(new CheaterData(chr.getCheatTracker().getPoints(), MapleCharacterUtil.makeMapleReadable(chr.getName()) + "(編號:" + chr.getId() + ") 檢測次數(" + chr.getCheatTracker().getPoints() + ") " + chr.getCheatTracker().getSummary() + " 地圖:" + chr.getMap().getMapName()));
                }
            }
        } finally {
            rL.unlock();
        }
        return cheaters;
    }

    public final List<CheaterData> getReports() {
        final List<CheaterData> cheaters = new ArrayList<>();

        rL.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getReportPoints() > 0) {
                    cheaters.add(new CheaterData(chr.getReportPoints(), MapleCharacterUtil.makeMapleReadable(chr.getName()) + " (" + chr.getReportPoints() + ") " + chr.getReportSummary()));
                }
            }
        } finally {
            rL.unlock();
        }
        return cheaters;
    }

    public final void disconnectAll() {
        disconnectAll(false);
    }

    public final void disconnectAll(final boolean checkGM) {
        wL.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (!chr.isGM() || !checkGM) {
                    chr.getClient().disconnect(false, false, true);
                    chr.getClient().getSession().close();
                    World.Find.forceDeregister(chr.getId(), chr.getName());
                    itr.remove();
                }
            }
        } finally {
            wL.unlock();
        }
    }

    public final String getOnlinePlayers(final boolean byGM) {
        final StringBuilder sb = new StringBuilder();

        if (byGM) {
            rL.lock();
            try {
                final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
                while (itr.hasNext()) {
                    sb.append(MapleCharacterUtil.makeMapleReadable(itr.next().getName()));
                    sb.append(", ");
                }
            } finally {
                rL.unlock();
            }
        } else {
            rL.lock();
            try {
                final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
                MapleCharacter chr;
                while (itr.hasNext()) {
                    chr = itr.next();

                    if (!chr.isGM()) {
                        sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                        sb.append(", ");
                    }
                }
            } finally {
                rL.unlock();
            }
        }
        return sb.toString();
    }

    public final void broadcastPacket(final byte[] data) {
        rL.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            while (itr.hasNext()) {
                itr.next().getClient().sendPacket(data);
            }
        } finally {
            rL.unlock();
        }
    }

    public final void broadcastSmegaPacket(final byte[] data) {
        rL.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getClient().isLoggedIn() && chr.getSmega()) {
                    chr.getClient().sendPacket(data);
                }
            }
        } finally {
            rL.unlock();
        }
    }

    public final void broadcastGMPacket(final byte[] data) {
        rL.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getClient().isLoggedIn() && chr.isIntern()) {
                    chr.getClient().sendPacket(data);
                }
            }
        } finally {
            rL.unlock();
        }
    }

    public final void deregisterPendingPlayerByAccountId(final int accountId) {
        wL.lock();
        try {
            for (CharacterTransfer transfer : PendingCharacter.values()) {
                if (transfer.accountid == accountId) {
                    PendingCharacter.remove(transfer.characterid);
                }
            }
        } finally {
            wL.unlock();
        }
    }

    public class PersistingTask implements Runnable {

        @Override
        public void run() {
            wL2.lock();
            try {
                final long currenttime = System.currentTimeMillis();
                final Iterator<Map.Entry<Integer, CharacterTransfer>> itr = PendingCharacter.entrySet().iterator();

                while (itr.hasNext()) {
                    if (currenttime - itr.next().getValue().TranferTime > 40000) { // 40 sec
                        itr.remove();
                    }
                }
            } finally {
                wL2.unlock();
            }
        }
    }
}
