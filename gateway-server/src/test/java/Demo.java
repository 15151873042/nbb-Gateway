import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class Demo {

    public static void main(String[] args) {
        List<String> list1 = new ArrayList<>();
        list1.add("1");
        list1.add("2");
        list1.add("3");
        List<String> list2 = new ArrayList<>();
        list2.add("a");
        list2.add("b");
        list2.add("c");

        List<List<String>> listAll = new ArrayList<>();
        listAll.add(list1);
        listAll.add(list2);

        Flux<List<String>> listFlux = Flux.fromIterable(listAll);
        Flux<String> stringFlux = listFlux.flatMap(list -> {
            return Flux.fromIterable(list);
        });

        String block1 = stringFlux.next().block();
        System.out.println(block1);

//        Mono<List<String>> listMono = stringFlux.collectList();
//        List<String> block = listMono.block();
//        System.out.println(block);

    }
}
