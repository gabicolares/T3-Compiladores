import java.util.ArrayList;
import java.util.List;

public class TabSimb {
    private final List<TS_entry> lista = new ArrayList<>();

    public void insert(TS_entry nodo) {
        lista.add(nodo);
    }

    public TS_entry pesquisa(String id) {
        for (TS_entry nodo : lista) {
            if (nodo.getId().equals(id)) {
                return nodo;
            }
        }
        return null;
    }

    public TS_entry pesquisa(String id, String escopo) {
        for (TS_entry nodo : lista) {
            if (nodo.getId().equals(id) && nodo.getEscopo().equals(escopo)) {
                return nodo;
            }
        }
        return null;
    }

    public TS_entry pesquisa(String id, String escopo, ClasseID categoria) {
        for (TS_entry nodo : lista) {
            if (nodo.getId().equals(id)
                    && nodo.getEscopo().equals(escopo)
                    && nodo.getCategoria() == categoria) {
                return nodo;
            }
        }
        return null;
    }

    public TS_entry pesquisaClasse(String id) {
        return pesquisa(id, "global", ClasseID.NomeClasse);
    }

    public boolean existeNoEscopo(String id, String escopo) {
        return pesquisa(id, escopo) != null;
    }

    public List<TS_entry> getLista() {
        return lista;
    }

    public void listar() {
        System.out.println("\nTabela de símbolos:");
        for (TS_entry nodo : lista) {
            System.out.println("  " + nodo);
        }
    }
}
