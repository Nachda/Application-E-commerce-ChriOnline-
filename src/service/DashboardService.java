package service;

import dao.DashboardDAO;
import model.DashboardSummary;

/**
 * Service qui calcule les métriques du tableau de bord administrateur.
 */
public class DashboardService {

    private final DashboardDAO dashboardDAO;

    public DashboardService() {
        this.dashboardDAO = new DashboardDAO();
    }

    /**
     * Retourne un objet DashboardSummary contenant toutes les métriques
     * (produits, commandes, revenus, notifications non lues).
     */
    public DashboardSummary getDashboardSummary() {
        return dashboardDAO.fetchSummary();
    }
}