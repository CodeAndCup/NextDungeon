package fr.perrier.dungeons.bungee.webserver;

import java.util.concurrent.CompletableFuture;

public interface ServerConnection {

    /**
     * Envoie une requête au serveur Spigot
     */
    CompletableFuture<String> sendRequest(String action, String data);

    /**
     * Vérifie si la connexion est active
     */
    boolean isConnected();

    /**
     * Récupère le nombre de joueurs connectés
     */
    int getPlayerCount();

    /**
     * Récupère le nombre maximum de joueurs
     */
    int getMaxPlayers();

    /**
     * Récupère le timestamp du dernier ping
     */
    long getLastPing();

    /**
     * Envoie un ping au serveur
     */
    void ping();

    /**
     * Ferme la connexion
     */
    void disconnect();
}
