contract C {
    function external_test_function() external {}
    function comparison_operator_for_external_function_with_extra_slots() external returns (bool) {
        return (
        (this.external_test_function{gas: 4} == this.external_test_function) &&
    (this.external_test_function{gas: 4} == this.external_test_function{gas: 4})
        );
    }
}