﻿/*
	Keroben - Leafre Cave of life - Entrance
*/

var morph;
var status = -1;

function action(mode, type, selection) {
    if (mode == 1) {
	status++;
    } else {
	status--;
    }
    if (status == 0) {
	    var marr = cm.getQuestRecord(160100);
	    var data = marr.getCustomData();
	    if (data == null) {
		marr.setCustomData("0");
	        data = "0";
	    }
	    var time = parseInt(data);
	morph = cm.getMorphState();
	if (morph == 2210003 || (cm.isQuestFinished(7301) && time + (6 * 3600000) < cm.getCurrentTime())) {
	    cm.sendNext("哦，我的兄弟！不要擔心人類的入侵。我會保護你的。進來。");
	} else {
	    var hp = cm.getPlayerStat("HP");
	    if (hp > 500) {
		cm.addHP(-500);
	    } else if (hp > 1 && hp <= 500) {
		cm.addHP(-(hp - 1));
	    }
	    cm.sendNext("這是遠遠不夠的，人類！任何人不得超過此點。從這裡滾開！");
	}
    } else if (status == 1) {
	if (morph == 2210003 || (cm.isQuestFinished(7301) && time + (6 * 3600000) < cm.getCurrentTime())) {
	    cm.cancelItem(2210003);
	    cm.warp(240050000, 0);

/*	    if (cm.haveItem(4031454)) { // Paladin
		cm.gainItem(4031454, -1);
		cm.gainItem(4031455, 1);
	    }*/
/*	    if (cm.getQuestStatus(6169) == 1) { // Temporary
		cm.gainItem(4031461, 1);
	    }*/
	} else {
	    cm.warp(240040600, "st00");
	}
	cm.dispose();
    }
}