import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TS_entry {
    private final String id;
    private final TypeInfo tipo;
    private final ClasseID categoria;
    private final String escopo;
    private TS_entry superClasse;
    private final List<TS_entry> parametros = new ArrayList<>();

    public TS_entry(String id, TypeInfo tipo, ClasseID categoria, String escopo) {
        this.id = id;
        this.tipo = tipo;
        this.categoria = categoria;
        this.escopo = escopo;
    }

    public String getId() {
        return id;
    }

    public TypeInfo getTipo() {
        return tipo;
    }

    public ClasseID getCategoria() {
        return categoria;
    }

    public String getEscopo() {
        return escopo;
    }

    public TS_entry getSuperClasse() {
        return superClasse;
    }

    public void setSuperClasse(TS_entry superClasse) {
        this.superClasse = superClasse;
    }

    public void addParametro(TS_entry parametro) {
        parametros.add(parametro);
    }

    public List<TS_entry> getParametros() {
        return Collections.unmodifiableList(parametros);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TS_entry{")
          .append("id='").append(id).append('\'')
          .append(", tipo=").append(tipo)
          .append(", categoria=").append(categoria)
          .append(", escopo='").append(escopo).append('\'');

        if (superClasse != null) {
            sb.append(", superClasse=").append(superClasse.getId());
        }

        if (!parametros.isEmpty()) {
            sb.append(", parametros=[");
            for (int i = 0; i < parametros.size(); i++) {
                TS_entry parametro = parametros.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(parametro.getTipo()).append(" ").append(parametro.getId());
            }
            sb.append("]");
        }

        sb.append('}');
        return sb.toString();
    }
}
