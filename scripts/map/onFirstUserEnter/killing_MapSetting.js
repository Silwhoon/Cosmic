function start(ms) {
    const PacketCreator = Java.type('tools.PacketCreator');
    const PyramidProcessor = Java.type('server.partyquest.pyramid.PyramidProcessor');
    var pyramid = PyramidProcessor.getPyramidForCharacter(ms.getPlayer().getId());
    var timeInSeconds = 180;
    if (pyramid != null) {
        timeInSeconds = pyramid.getTotalTime();
    }
    ms.getPlayer().resetEnteredScript();
    ms.getPlayer().sendPacket(PacketCreator.getClock(timeInSeconds));
}