package client;

import handling.world.World;
import server.MapleInventoryManipulator;
import server.Randomizer;
import tools.packet.CField;
import tools.packet.CWvsContext;

public class RockPaperScissors {

    private int round = 0;
    private boolean ableAnswer = true;
    private boolean win = false;

    public RockPaperScissors(final MapleClient c, final byte mode) {
        c.sendPacket(CField.getRPSMode((byte) (0x09 + mode), -1, -1, -1));
        if (mode == 0) {
            c.getPlayer().gainMeso(-1000, true, true);
        }
    }

    public final boolean answer(final MapleClient c, final int answer) {
        if (ableAnswer && !win && answer >= 0 && answer <= 2) {
            final int response = Randomizer.nextInt(3);
            if (response == answer) {
                c.sendPacket(CField.getRPSMode((byte) 0x0B, -1, (byte) response, (byte) round));
                //dont do anything. they can still answer once a draw
            } else if ((answer == 0 && response == 2) || (answer == 1 && response == 0) || (answer == 2 && response == 1)) { //they win
                c.sendPacket(CField.getRPSMode((byte) 0x0B, -1, (byte) response, (byte) (round + 1)));
                ableAnswer = false;
                win = true;
            } else { //they lose
                c.sendPacket(CField.getRPSMode((byte) 0x0B, -1, (byte) response, (byte) -1));
                ableAnswer = false;
            }
            return true;
        }
        reward(c);
        return false;
    }

    public final boolean timeOut(final MapleClient c) {
        if (ableAnswer && !win) {
            ableAnswer = false;
            c.sendPacket(CField.getRPSMode((byte) 0x0A, -1, -1, -1));
            return true;
        }
        reward(c);
        return false;
    }

    public final boolean nextRound(final MapleClient c) {
        if (win) {
            round++;
            if (round < 10) {
                win = false;
                ableAnswer = true;
                c.sendPacket(CField.getRPSMode((byte) 0x0C, -1, -1, -1));
                return true;
            }
        }
        reward(c);
        return false;
    }

    public final void reward(final MapleClient c) {
        int fuck = round + 1;
        if (win) {
            MapleInventoryManipulator.addById(c, 4031332 + round, (short) 1, "", null, 0, "");
            if (fuck >= 5)
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "『猜拳PK』" + c.getPlayer().getName() + "從猜拳王贏得了" + fuck + "場勝利！"));
        } else if (round == 0) {
            c.getPlayer().gainMeso(500, true, true);
        }
        c.getPlayer().setRPS(null);
    }

    public final void dispose(final MapleClient c) {
        reward(c);
        c.sendPacket(CField.getRPSMode((byte) 0x0D, -1, -1, -1));
    }
}
