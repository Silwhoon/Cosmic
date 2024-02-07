function start(ms) {
    const PacketCreator = Java.type('tools.PacketCreator');
    var pq = ms.getPyramid();
    ms.getPlayer().resetEnteredScript();
    ms.getPlayer().sendPacket(PacketCreator.getClock(pq.timer()));
}