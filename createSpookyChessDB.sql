/*
Benjamin Ma; benjamjm@usc.edu; 6386770787
CS201 Final Project
*/

DROP DATABASE IF EXISTS SpookyChess;

CREATE DATABASE SpookyChess;

USE SpookyChess;
CREATE TABLE Users (
	userID INT(5) PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL,
    password VARCHAR(30) NOT NULL,
    wins INT(5) NOT NULL,
    losses INT(5) NOT NULL
);
            
CREATE TABLE Games (
	gameID INT(7) PRIMARY KEY AUTO_INCREMENT,
    gameURL VARCHAR(30) NULL,
    password VARCHAR(30) NOT NULL,
	player1 INT(5) NOT NULL,
    player2 INT(5) NOT NULL,
    FOREIGN KEY fk1(player1) REFERENCES Users(userID),
    FOREIGN KEY fk2(player2) REFERENCES Users(userID)
);