CREATE TABLE `cosmic`.`quest_reward_custom`
(
    `id`          INT NOT NULL AUTO_INCREMENT,
    `characterId` INT NOT NULL,
    `rewardId`    INT NOT NULL,
    PRIMARY KEY (`id`)
);
