function start(ms) {
    const PyramidProcessor = Java.type('server.partyquest.pyramid.PyramidProcessor');
    var pyramid = PyramidProcessor.getPyramidForCharacter(ms.getPlayer().getId());
    if (pyramid != null) {
        pyramid.broadcastScore(ms.getPlayer());
    }
}