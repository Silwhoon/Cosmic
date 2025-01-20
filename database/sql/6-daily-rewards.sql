CREATE TABLE `cosmic`.`daily_rewards`
(
    `id`                INT    NOT NULL AUTO_INCREMENT,
    `characterId`       INT    NOT NULL,
    `lastClaimedReward` BIGINT NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`)
);


CREATE TABLE `cosmic`.`daily_rewards`
(
    `characterId`       int    NOT NULL,
    `lastClaimedReward` bigint NOT NULL DEFAULT '0',
    PRIMARY KEY (`characterId`)
);
