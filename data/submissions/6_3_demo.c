#include <stdio.h>
#include <stdlib.h>

// you can define helper functions you need
typedef struct 
{
    int *array;
    int num_of_element;
    int size;
    int curr_idx;
    // declare variables you need
} arrayList;

void error(arrayList *list){
    printf("Error\n");
    if(list->array)
        free(list->array);
    exit(1);
}
void init(arrayList* list)
{
    // implement initialization
    list->size = 2;
    list->array = (int*)malloc(2 * sizeof(int));
    if(!list->array){
        error(list);
    }
    list->num_of_element = 0;
    list->curr_idx = -1;
}

void free_list(arrayList* list)
{
    // implement destruction of list
    if(list->array)
        free(list->array);
    list->array = NULL;
}

void increase_capacity(arrayList* list)
{
    // implement capacity increase
    int prev_size = list->size;
    list->size = list->size * 2;
    int *tmp = (int *) realloc(list->array, list->size * sizeof(int));
    if(!tmp){
        error(list);
    }
    list->array = tmp;
    printf("Capacity increased from %d to %d\n", prev_size, list->size);
}

void decrease_capacity(arrayList* list)
{
    // implement capacity decrease
    if(list->size <= 2)
        return;
    
    int prev_size = list->size;
    list->size = list->size / 2;

    list->array = (int*)realloc(list->array, list->size * sizeof(int));
    printf("Capacity decreased from %d to %d\n", prev_size, list->size);
}

void print(arrayList* list)
{
    // implement list printing
    if(list->num_of_element == 0){
        printf("The list is empty\n");
        return;
    }
    int n = list->num_of_element;
    printf("[");
    for(int i=0; i<n; i++){
        if(i != list->curr_idx)
            printf("%d",list->array[i]);
        else
            printf("%d|",list->array[i]);

        if(i!=n-1)
            printf(" ");
    }
    
    printf("]\n");
}

void insert(int item, arrayList* list)
{
    // implement insert function
    if(list->num_of_element+1 > list->size / 2)
        increase_capacity(list);
    list->curr_idx++;
    list->num_of_element++;
    for(int i=list->curr_idx + 1; i<list->num_of_element; i++)
        list->array[i] = list->array[i - 1];
    list->array[list->curr_idx] = item;

}

int delete_cur(arrayList* list)
{
    // implement deletion of element at current index position
    if(list->num_of_element == 0)
        return -1;
    int del_data = list->array[list->curr_idx];
    if(list->curr_idx == list->num_of_element - 1){
        list->curr_idx--;
        list->num_of_element--;
    }
    else{
        for(int i=list->curr_idx; i<list->num_of_element - 1; i++)
        list->array[i+1] = list->array[i];
        list->num_of_element--;
    }
    if(list->num_of_element < list->size/4)
        decrease_capacity(list);
    
    return del_data;
}

void append(int item, arrayList* list)
{
    // implement append function
    if(list->num_of_element + 1 > list->size/2)
        increase_capacity(list);
    list->array[list->num_of_element++] = item;
    if(list->num_of_element == 1)
        list->curr_idx = 0;
    
}

int size(arrayList* list)
{
    // implement size function
    return list->num_of_element;
}

void prev(int n, arrayList* list)
{
    // implement prev function
    list->curr_idx -= n;
    if(list->curr_idx < 0)
        list->curr_idx = 0;
}

void next(int n, arrayList* list)
{
    // implement next function
    list->curr_idx += n;
    if(list->curr_idx >= list->num_of_element)
        list->curr_idx = list->num_of_element - 1;
}

int is_present(int n, arrayList* list)
{
    // implement presence checking function
    for(int i=0; i<list->num_of_element; i++){
        if(list->array[i] == n) 
            return 1;
    }
    return 0;
}

void clear(arrayList* list)
{
    // implement list clearing function
    free_list(list);
    init(list);
}

int delete_item(int item, arrayList* list)
{
    // implement item deletion function
    int idx = search(item, list);
    if(idx == -1) {
        printf("%d not found\n", item);
        return -1;
    }
    for (int i = idx; i < list->num_of_element - 1; i++)
        list->array[i] = list->array[i + 1];
    list->curr_idx = idx;
    list->num_of_element--;
    if(list->num_of_element < list->size/4)
        decrease_capacity(list);
}

void swap_ind(int ind1, int ind2, arrayList* list)
{
    // implement swap function at metioned index position
    if(list->num_of_element == 0 || ind1>=list->num_of_element || ind2>=list->num_of_element) return;
    int temp = list->array[ind1];
    list->array[ind1] = list->array[ind2];
    list->array[ind2] = temp;
}

int search(int item, arrayList* list)
{
    // implement search function
    for(int i=0; i<list->num_of_element; i++){
        if(list->array[i] == item) 
            return i;
    }
    return -1;
}

int find(int ind, arrayList* list)
{
    // implement find function
    if(ind >= list->num_of_element) return -1;
    return list->array[ind];
}

int update(int ind, int value, arrayList* list)
{
    // implement update function at metioned index position
    if(ind >= list->num_of_element) return -1;
    int temp = list->array[ind];
    list->array[ind] = value;
    return temp;
}

int trim(arrayList* list)
{
    // implement trim function
    if(list->num_of_element == 0){
        printf("List is empty\n");
        return -1;
    }
    int val = list->array[list->num_of_element - 1];
    list->num_of_element--;

    if(list->num_of_element < list->size/4)
        decrease_capacity(list);
    if(list->num_of_element < list->curr_idx)
        list->curr_idx = list->num_of_element - 1;
    
    return val;
}

void reverse(arrayList* list)
{
    // implement reverse function
    if(list->num_of_element == 0)
        return;
    int l=0, r=list->num_of_element-1;
    while(l<r){
        int temp = list->array[l];
        list->array[l] = list->array[r];
        list->array[r] = temp;
        l++, r--;
    }
}

// you can define helper functions you need
int main(){
    arrayList list;
    int cmnd = 0;
    do{
        printf("enter cmnd : ");
        scanf("%d",&cmnd);
        if(cmnd == 1){
            int val;
            scanf("%d",&val);
            insert(val, &list);
            print(&list);
        }
        else if(cmnd == 2){
            delete_cur(&list);
            print(&list);
        }
        else if(cmnd == 3){
            int val;
            scanf("%d",&val);
            append(val, &list);
            print(&list);
        }
        else if(cmnd == 4){
            printf("%d -> size\n", size(&list));
        }
        else if(cmnd == 5){
            int n;scanf("%d",&n);
            prev(n, &list);
        }
        else if(cmnd == 6){
            int n;scanf("%d",&n);
            next(n, &list);
        }
        else if(cmnd == 7){
            int n;scanf("%d",&n);
            if(is_present(n, &list)){
                printf("%d is present\n", n);
            }
            else{
                printf("%d isnot present\n", n);
            }
        }
        else if(cmnd == 8){
            clear(&list);
        }
        else if(cmnd == 9){
            int val;scanf("%d",&val);
            delete_item(val, &list);
            print(&list);
        }
        else if(cmnd == 10){
            int idx, idxx;
            scanf("%d %d",&idx, &idxx);
            swap_ind(idx,idxx,&list);
            print(&list);
        }
        else if(cmnd == 11){
            int val;scanf("%d",&val);
            int found = search(val, &list);
            if(found == -1) printf("%d not found\n", val);
            else printf("%d found at idx->%d\n", val, found);
        }
        else if(cmnd == 12){
            int idx;scanf("%d",&idx);
            int found = find(idx, &list);
            if(found == -1) printf("Not found\n");
            else printf("%d exists at %d\n", found, idx);
        }
        else if(cmnd == 13){
            int idx, val;
            scanf("%d %d",&idx, &val);
            int ret = update(idx, val, &list);
            printf("%d was replaced by %d\n", ret, val);
        }
        else if(cmnd == 14){
            int ret = trim(&list);
            printf("%d was cut off\n", ret);
        }
        else if(cmnd == 15){
            reverse(&list);
            print(&list);
        }
    }while(cmnd != -1);
}