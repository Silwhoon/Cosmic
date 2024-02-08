function start(ms) {
    const PacketCreator = Java.type('tools.PacketCreator');
    const PyramidProcessor = Java.type('server.partyquest.pyramid.PyramidProcessor');
    var pyramid = PyramidProcessor.getPyramidForCharacter(ms.getPlayer().getId());

    // TODO: Add the "BONUS STAGE" effect

    ms.getPlayer().resetEnteredScript();
    ms.getPlayer().sendPacket(PacketCreator.getClock(60));
}