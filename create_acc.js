document.addEventListener("DOMContentLoaded",function(){
    let form= document.querySelector(".form-group form");

    form.addEventListener("submit", function(event){
        event.preventDefault();
        const message=document.getElementById('message');
        
        message.textContent= `Thank you ${fName} ${lName}! Your Account has been Created! `;
        form.reset();

    });
});