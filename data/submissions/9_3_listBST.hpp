#ifndef LISTBST_H
#define LISTBST_H

#include "BST.hpp"
#include <iostream>
#include <stdexcept>
using namespace std;
/**
 * Binary Search Tree implementation using linked list structure
 * 
 * @tparam Key - The type of keys stored in the BST
 * @tparam Value - The type of values associated with keys//
 */
template <typename Key, typename Value>
class ListBST : public BST<Key, Value> {
private:
    /**
     * Node class for the binary search tree
     */
    class Node {
    public:
        Key key;
        Value value;
        Node* left;
        Node* right;
        
        Node(Key k, Value v) : key(k), value(v), left(nullptr), right(nullptr) {}
    };
    
    Node* root;
    size_t node_count;
    
    // TODO: Implement private helper functions as needed
    // Start your private helper functions here
    void print_default(Node *node) const{
        if(!node)
            return;
        cout << "(" << node->key << ":" << node->value;
        if(node->left){
            cout << " ";
            print_default(node->left);
        }
        else if(!node->left && node->right){
            cout << " () ";
        }
        if(node->right){
            cout << " ";
            print_default(node->right);
        }
        cout << ")";
    }
    void print_inorder(Node *node) const {
        if(!node)
            return;
        print_inorder(node->left);
        cout << "(" << node->key << ":" << node->value << ") ";
        print_inorder(node->right);
    }
    void print_postorder(Node *node) const{
        if(!node)
            return;
        print_postorder(node->left);
        print_postorder(node->right);
        cout << "(" << node->key << ":" << node->value << ") ";
    }
    void print_preorder(Node *node) const{
        if(!node)
            return;
        cout << "(" << node->key << ":" << node->value << ") ";
        print_preorder(node->left);
        print_preorder(node->right);
    }
    // void print_item(Node* node) const{
    //     if(!node)
    //         return;
    //     print_item(node->left);
    //     cout << "(" << node->key << ":" << node->value << ")";
    //     print_item(node->right);
    // }
    void print_report(Node* node) const{
        if(!node)
            return;
        print_report(node->left);
        cout << " " << node->key << ": " << node->value << "\n";
        print_report(node->right);
    }
    void clear_tree(Node * node){
        if(!node)
            return;
        clear_tree(node->left);
        clear_tree(node->right);
        delete node;
    }
    Node *find_min(Node *node){
        while(node && node->left)
            node = node->left;
        return node;
    }
    Node *remove(Node *node, Key key, bool &removed){
        if(!node)
            return NULL;
        if(key < node->key)
            node->left = remove(node->left, key, removed);
        else if(key > node->key)
            node->right = remove(node->right , key, removed);
        else{
            removed = true;
            if(!node->left){
                Node *r = node->right;
                delete node;
                return r;
            }
            else if(!node->right){
                Node *l = node->left;
                delete node;
                return l;
            }

            Node* successor = find_min(node->right);
            node->key = successor->key;
            node->value = successor->value;
            node->right = remove(node->right, successor->key, removed);
        }
        return node;
    }
    // End your private helper functions here

public:
    /**
     * Constructor
     */
    ListBST() : root(nullptr), node_count(0) {}
    
    /**
     * Destructor
     */
    ~ListBST() {
        // TODO: Implement destructor to free memory
        clear_tree(root);
    }
    
    /**
     * Insert a key-value pair into the BST
     */
    bool insert(Key key, Value value) override {
        // TODO: Implement insertion logic
        Node * newnode = new Node(key, value);
        if(!root){
            root = newnode;
            node_count++;
            return true;
        }
        Node* curr = root;
        Node* par = NULL;
        while(curr){
            par = curr;
            if(key < curr->key){
                curr = curr->left;
            }
            else if(key > curr->key){
                curr = curr->right;
            }
            else{
                delete newnode;
                return false;
            }
        }
        if(key < par->key){
            par->left = newnode;
        }
        else
            par->right = newnode;
        
        node_count++;
        return true;
    }
    
    /**
     * Remove a key-value pair from the BST
     */
    bool remove(Key key) override {
        // TODO: Implement removal logic
        bool removed = false;
        root = remove(root, key, removed);
        if(removed)
            node_count--;
        return removed;
    }
    
    /**
     * Find if a key exists in the BST
     */
    bool find(Key key) const override {
        // TODO: Implement find logic
        Node *tmp = root;
        while (tmp){
            if(tmp->key == key)
                return true;
            else if(tmp->key < key)
                tmp = tmp->right;
            else tmp = tmp->left;
        }
        return false;
    }

    /**
     * Find a value associated with a given key
     */
    Value get(Key key) const override {
        // TODO: Implement get logic
        if(!find(key)){
            throw runtime_error("Key not found\n");
        }
        Node* curr = root;
        while (curr){
            if(key == curr->key)
                return curr->value;
            else if(key < curr->key)
                curr = curr->left;
            else
                curr = curr->right;
        }
        throw runtime_error("Something is wrong\n");
    }

    /**
     * Update the value associated with a given key
     */
    void update(Key key, Value value) override {
        // TODO: Implement update logic
        Node *curr = root;
        while(curr){
            if(curr->key == key){
                curr->value = value;
                return;
            }
            else if(curr->key > key)
                curr = curr->left;
            else curr = curr->right;
        }
        throw runtime_error("Key not found\n");
    }

    /**
     * Clear all elements from the BST
     */
    void clear() override {
        // TODO: Implement clear logic
        clear_tree(root);
        root = NULL;
        node_count = 0;
    }
    
    /**
     * Get the number of keys in the BST
     */
    size_t size() const override {
        // TODO: Implement size logic
        return node_count;
    }
    
    /**
     * Check if the BST is empty
     */
    bool empty() const override {
        // TODO: Implement empty check logic
        return root == NULL;
    }
    
    /**
     * Find the minimum key in the BST
     */
    Key find_min() const override {
        // TODO: Implement find_min logic
        if(!root){
            throw runtime_error("BST empty\n");
        }
        Node *tmp = root;
        while(tmp && tmp->left)
            tmp = tmp->left;
        return tmp->key;
    }
    
    /**
     * Find the maximum key in the BST
     */
    Key find_max() const override {
        // TODO: Implement find_max logic
        if(!root){
            throw runtime_error("BST empty\n");
        }
        Node *tmp = root;
        while(tmp && tmp->right)
            tmp = tmp->right;
        return tmp->key;
    }

    /**
     * Print the BST using specified traversal method
     */
    void print(char traversal_type = 'D') const override {
        // TODO: Implement print logic
        if(!root){
            cout << "Empty\n";
            return;
        }
        if(traversal_type == 'D'){
            cout << "BST (Default): ";
            print_default(root);
        }
        else if(traversal_type == 'I'){
            cout << "BST (In-order): ";
            print_inorder(root);
        }
        else if(traversal_type == 'P'){
            cout << "BST (Pre-order): ";
            print_preorder(root);
        }
        else if(traversal_type == 'R'){
            print_report(root);
        }
        else{
            cout << "BST (Post-order): ";
            print_postorder(root);
        }
        cout << "\n";
    }
    
};

#endif // LISTBST_H